import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


//run artifacts java -cp ./db-to-struct.jar Main --init napouser,napouser,MataPena --tbl tbl_use
//run artifacts java -cp ./db-to-struct.jar Main --init napouser,napouser,MataPena --tbl all

public class Main {

    static final String INIT = "--init";
    static final String TABLE = "--tbl";

    public static void main(String[] args) {
        String db, usr, pwd, tbl;
        int initIdx = 0, tblIdx = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(INIT)) initIdx = i;
            if (args[i].equals(TABLE)) tblIdx = i;
        }

        String[] databaseInitial = args[initIdx + 1].split(",");
        usr = databaseInitial[0];
        pwd = databaseInitial[1];
        db = databaseInitial[2];
        tbl = args[tblIdx + 1];

        System.out.println("db : " + db);
        System.out.println("usr : " + usr);
        System.out.println("pwd : " + pwd);
        System.out.println("tbl : " + tbl);


        Connection connection;

        try {
            connection = makeConnection(usr, pwd, db);
            if (tbl.equals("all")) {
                Connection finalConnection = connection;
                tableScan(connection, db).forEach(s -> {
                    connectToTable(finalConnection, s);
                });
            } else {
                connectToTable(connection, tbl);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Connection makeConnection(String... initial) throws SQLException, ClassNotFoundException {
        String usr = initial[0];
        String pwd = initial[1];
        String db = initial[2];


        Connection connect;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager
                    .getConnection("jdbc:mysql://localhost/" + db + "?"
                            + "user=" + usr +
                            "&password=" + pwd);
            System.out.println("Connected");
            return connect;
        } catch (Exception e) {
            throw e;
        }
    }

    private static List<String> tableScan(Connection connection, String db) {
        String query = "SHOW TABLES IN " + db + ";";
        List<String> result = new ArrayList<>();

        Statement st = null;
        try {
            st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                String table = rs.getString("tables_in_" + db);
                System.out.println(table);
                result.add(table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


    private static void connectToTable(Connection connection, String tbl) {
        String query = "DESCRIBE " + tbl;
        List<SchemeResult> schemeResults = new ArrayList<>();

        Statement st = null;
        try {
            st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                String field = rs.getString("Field");
                String type = rs.getString("Type");
                String key = rs.getString("Key");

                System.out.println(field);
                SchemeResult schemeResult = new SchemeResult();
                schemeResult.field = field;
                schemeResult.type = type;
                schemeResult.key = key;
                schemeResults.add(schemeResult);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }


        System.out.println("schemeResults.size() : " + schemeResults.size());
        schemeResults.forEach(schemeResult -> {
            System.out.println(schemeResult.toString());
        });

        createStructFile(schemeResults, tbl);
    }


    static void createStructFile(List<SchemeResult> resulst, String tbl) {
        try {
            File file = new File(tbl + ".go");
            if (file.exists()) {
                System.out.println("File already exists.");
                file.delete();
                System.out.println("File deleted.");
            }
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
                writeStructFile(resulst, file);
            } else {
                System.out.println("File Not Created.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    static void writeStructFile(List<SchemeResult> resulst, File file) {
        String tblName = file.getName().replace(".go", "");
        String className = toCamelCase(tblName);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write("package models\n\n\n");
            writer.write("type " + className + " struct {\n");
            resulst.forEach(schemeResult -> {
                boolean isPrimaryKey = schemeResult.key.toLowerCase().contains("pri");
                String field = schemeResult.field;
                String type = schemeResult.type.toLowerCase();
                String json = "`json:\"" + field + "\"`";
                if (isPrimaryKey) {
                    json = "`gorm:\"primary_key\" json:\"" + field + "\"`";
                }

                try {
                    String tmpType = type.contains("int") ? "int" : (type.contains("varchar") ? "string" : "interface{}");
                    writer.write("  " + toCamelCase(field) + " " + tmpType + " " + json + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.write("}\n\n\n");
            writer.write("func (" + className + ") TableName() string {\n");
            writer.write("  return \"" + tblName + "\"\n");
            writer.write("}");

            writer.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    static String toCamelCase(String s) {
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }

    static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    static class SchemeResult {
        String field;
        String type;
        String key;

        @Override
        public String toString() {
            return "SchemeResult{" +
                    "field='" + field + '\'' +
                    ", type=" + type +
                    ", key='" + key + '\'' +
                    '}';
        }
    }
}

