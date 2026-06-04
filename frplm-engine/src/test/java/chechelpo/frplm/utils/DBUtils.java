package chechelpo.frplm.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

public final class DBUtils {
    private DBUtils() {}

    static Connection newConnection() throws Exception {
        return DriverManager.getConnection(
                "jdbc:h2:mem:test_db;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
    }


}
