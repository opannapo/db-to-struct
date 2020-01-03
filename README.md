# db-to-struct
Auto Create Golang Struct (Models) from local database


# How to use 
1. Build this project as java project.
2. Execute JAR file, (look at the output directory : db-to-struct/db-to-struct/out/artifacts/db_to_struct_jar/db_to_struct.jar)

## Command Format :
--init <db_user>,<db_password>,<database> --tbl <table_name>

### 1. Create for selected table only
--init <db_user>,<db_password>,<database> --tbl <table_name>

### 2. Create for all tables on database
--init <db_user>,<db_password>,<database> --tbl all
