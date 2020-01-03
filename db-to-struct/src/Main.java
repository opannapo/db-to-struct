import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


//java -cp ./db-to-struct.jar Main --init napouser,napouser,MataPena --tbl tbl_use
//java -cp ./db-to-struct.jar Main --init napouser,napouser,MataPena --tbl all

public class Main {

    private static final String INIT = "--init";
    private static final String TABLE = "--tbl";
    private static final String PACKAGE = "--package";

    public static void main(String[] args) throws Throwable {
        String db, usr, pwd, tbl, pkg;
        int initIdx = -1, tblIdx = -1, pkgIdx = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(INIT)) initIdx = i;
            if (args[i].equals(TABLE)) tblIdx = i;
            if (args[i].equals(PACKAGE)) pkgIdx = i;
        }

        if (initIdx < 0) throw (breakMessage("? database"));
        if (tblIdx < 0) throw (breakMessage("? table"));

        String[] databaseInitial = args[initIdx + 1].split(",");
        usr = databaseInitial[0];
        pwd = databaseInitial[1];
        db = databaseInitial[2];
        tbl = args[tblIdx + 1];
        pkg = pkgIdx > 0 ? args[pkgIdx + 1] : "models";


        System.out.println("\n\nInitial ..............................");
        System.out.println("Initial Connection db:" + db + ", usr:" + usr + ", pwd:" + pwd);
        System.out.println("Initial Table tbl:" + tbl);
        System.out.println("Initial Package:" + pkg);

        Connection connection;

        try {
            connection = makeConnection(usr, pwd, db);
            if (tbl.equals("all")) {
                Connection finalConnection = connection;
                tableScan(connection, db).forEach(s -> {
                    connectToTable(finalConnection, s, pkg);
                });
            } else {
                connectToTable(connection, tbl, pkg);
            }
            connection.close();
            System.out.println("Connection close " + connection.isClosed());
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Connection makeConnection(String... initial) throws SQLException, ClassNotFoundException {
        String usr = initial[0];
        String pwd = initial[1];
        String db = initial[2];


        System.out.println("\nConnection ..............................");
        System.out.println("Connecting...");
        Connection connect;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager
                    .getConnection("jdbc:mysql://localhost/" + db + "?"
                            + "user=" + usr +
                            "&password=" + pwd);
            System.out.println("Status Connected !");
            return connect;
        } catch (Exception e) {
            throw e;
        }
    }

    private static List<String> tableScan(Connection connection, String db) {
        System.out.println("\nTable in " + db + " ..............................");
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

    private static void connectToTable(Connection connection, String tbl, String pkg) {
        System.out.println("\nField on " + tbl + " ..............................");
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

                //System.out.println(field);
                SchemeResult schemeResult = new SchemeResult();
                schemeResult.field = field;
                schemeResult.type = type;
                schemeResult.key = key;
                schemeResults.add(schemeResult);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }


        schemeResults.forEach(schemeResult -> {
            System.out.println(schemeResult.toString());
        });

        createStructFile(schemeResults, tbl, pkg);
    }

    private static void createStructFile(List<SchemeResult> resulst, String tbl, String pkg) {
        System.out.println("\nCreate file " + tbl + ".go" + " ..............................");
        try {
            File file = new File(tbl + ".go");
            if (file.exists()) {
                System.out.println("File already exists.");
                file.delete();
                System.out.println("File deleted.");
            }
            if (file.createNewFile()) {
                System.out.println("File created " + file.getName());
                writeStructFile(resulst, file, pkg);
            } else {
                System.out.println("File Not Created.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void writeStructFile(List<SchemeResult> resulst, File file, String pkg) {
        System.out.println("\nWrite file " + file + " ..............................");
        String tblName = file.getName().replace(".go", "");
        String className = toCamelCase(tblName);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write("package " + pkg + "\n\n\n");
            writer.write("// " + className + " " + pkg + " @Auto-Generate\n"); //go-lint should have comment
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
                    writer.write("  " + goLintFieldId(field) + " " + tmpType + " " + json + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.write("}\n\n\n");
            writer.write("// TableName for " + tblName + " @Auto-Generate\n"); //go-lint should have comment
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

    private static String toCamelCase(String s) {
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    private static String goLintFieldId(String s) {
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            if (part.toLowerCase().equals("id")) {
                camelCaseString = camelCaseString +
                        part.toUpperCase();
            } else {
                camelCaseString = camelCaseString +
                        part.substring(0, 1).toUpperCase() +
                        part.substring(1).toLowerCase();
            }
        }
        return camelCaseString;
    }

    private static Throwable breakMessage(String msg) {
        return new Exception("Invalid operation " + msg +
                "\n Format -> --init [dbUser][dbPassword][dbName] --tbl [tblName] --package [packageName]");
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


