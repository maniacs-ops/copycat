/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.copycat.server.state;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.util.Assert;
import io.atomix.copycat.client.session.Session;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.session.Sessions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session manager.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
class ServerSessionManager implements Sessions {
  private final Map<UUID, Address> addresses = new ConcurrentHashMap<>();
  private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();
  final Map<Long, ServerSession> sessions = new ConcurrentHashMap<>();
  final Map<UUID, ServerSession> clients = new ConcurrentHashMap<>();
  final Set<SessionListener> listeners = new HashSet<>();

  @Override
  public Session session(long sessionId) {
    return sessions.get(sessionId);
  }

  @Override
  public Sessions addListener(SessionListener listener) {
    listeners.add(Assert.notNull(listener, "listener"));
    return this;
  }

  @Override
  public Sessions removeListener(SessionListener listener) {
    listeners.remove(Assert.notNull(listener, "listener"));
    return this;
  }

  /**
   * Registers an address.
   */
  ServerSessionManager registerAddress(UUID client, Address address) {
    ServerSession session = clients.get(client);
    if (session != null) {
      session.setAddress(address);
    }
    addresses.put(client, address);
    return this;
  }

  /**
   * Registers a connection.
   */
  ServerSessionManager registerConnection(UUID client, Connection connection) {
    ServerSession session = clients.get(client);
    if (session != null) {
      session.setConnection(connection);
    }
    connections.put(client, connection);
    return this;
  }

  /**
   * Unregisters a connection.
   */
  ServerSessionManager unregisterConnection(Connection connection) {
    Iterator<Map.Entry<UUID, Connection>> iterator = connections.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<UUID, Connection> entry = iterator.next();
      if (entry.getValue().equals(connection)) {
        ServerSession session = clients.get(entry.getKey());
        if (session != null) {
          session.setConnection(null);
        }
        iterator.remove();
      }
    }
    return this;
  }

  /**
   * Registers a session.
   */
  ServerSession registerSession(ServerSession session) {
    session.setAddress(addresses.get(session.client()));
    session.setConnection(connections.get(session.client()));
    sessions.put(session.id(), session);
    clients.put(session.client(), session);
    return session;
  }

  /**
   * Unregisters a session.
   */
  ServerSession unregisterSession(long sessionId) {
    ServerSession session = sessions.remove(sessionId);
    if (session != null) {
      clients.remove(session.client());
      addresses.remove(session.client());
      connections.remove(session.client());
    }
    return session;
  }

  /**
   * Gets a session by session ID.
   *
   * @param sessionId The session ID.
   * @return The session or {@code null} if the session doesn't exist.
   */
  ServerSession getSession(long sessionId) {
    return sessions.get(sessionId);
  }

  /**
   * Gets a session by client ID.
   *
   * @param clientId The client ID.
   * @return The session or {@code null} if the session doesn't exist.
   */
  ServerSession getSession(UUID clientId) {
    return clients.get(clientId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<Session> iterator() {
    return (Iterator) sessions.values().iterator();
  }

}
