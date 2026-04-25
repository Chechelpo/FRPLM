package chechelpo.demo.chat.messages;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MessagesTriggers implements Trigger {

    // messages(session_id, tick_num, location_world_id, location_id, actor_character_id, advances_time_by)
    private static final int COL_SESSION_ID = 0;
    private static final int COL_TICK_NUM = 1;

    @Override
    public void init(Connection conn,
                     String schemaName,
                     String triggerName,
                     String tableName,
                     boolean before,
                     int type) {
        // No-op
    }

    @Override
    public void fire(Connection conn,
                     Object[] oldRow,
                     Object[] newRow) throws SQLException {

        final Integer sessionId = (Integer) newRow[COL_SESSION_ID];
        if (sessionId == null) {
            throw new SQLException("messages.session_id must not be NULL");
        }

        final int tick;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT current_tick FROM sessions WHERE id = ?"
        )) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No sessions row for id=" + sessionId);
                }
                tick = rs.getInt(1);
            }
        }

        newRow[COL_TICK_NUM] = tick;

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sessions SET current_tick = current_tick + 1 WHERE id = ?"
        )) {
            ps.setInt(1, sessionId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Failed to advance sessions.current_tick for id=" + sessionId);
            }
        }
    }

    @Override
    public void close() { }

    @Override
    public void remove() { }
}