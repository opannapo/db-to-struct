# db-to-struct
Auto Create Golang Struct (Models) from local database


# How to use 
1. Build this project as java project.
2. Execute JAR file, (look at the output directory : db-to-struct/db-to-struct/out/artifacts/db_to_struct_jar/db_to_struct.jar)

## Command Format :
--init [db_user],[db_password],[database] --tbl [table_name]

### 1. Create for selected table only
--init [db_user],[db_password],[database] --tbl [table_name]

### 2. Create for all tables on database
--init [db_user],[db_password],[database] --tbl all

### 3. Optional Parameter
### --package
Example: --init root,root,MyDataBase --tbl tbl_user --package domain
<br>Without --package, package will be created as models


# Feature Update
- <b>Go-lint</b> -> Exported type should have comment or be unexported (go-lint)<br>
- <b>Go-lint</b> -> Struct field with "id" prefix should be UpperCase (go-lint)<br>

# Video (Example : How to use)
https://www.youtube.com/watch?v=2Q5F_4Yv4oU

<a href="https://www.youtube.com/watch?v=2Q5F_4Yv4oU" target="_blank">
<img src="https://img.youtube.com/vi/2Q5F_4Yv4oU/maxresdefault.jpg" 
alt="IMAGE ALT TEXT HERE" width="50%" height="50%" border="10" /></a>
 
