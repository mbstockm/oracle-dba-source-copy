import groovy.sql.Sql

import java.nio.file.Paths
import java.sql.Clob
import java.sql.ResultSet

@GrabConfig(systemClassLoader = true)
@Grab(group = 'com.oracle', module = 'ojdbc8', version = 'current')

Properties dbProps = new Properties()
Paths.get(System.properties.'user.home','.credentials','bannerProduction.properties').withInputStream {
    dbProps.load(it)
}


Sql.withInstance(dbProps.url,dbProps,dbProps.driver) { Sql sql ->

    sql.query("""select a.OWNER                                "Owner"
                       ,a.OBJECT_NAME                           "Name"
                       ,a.OBJECT_TYPE                          "Type"
                       ,(select count(*)
                           from dba_source x
                          where x.owner = a.OWNER
                            and x.name = a.OBJECT_NAME
                            and x.TYPE = a.OBJECT_TYPE)         "Lines"  
                      ,f_uc_object_source(a.OWNER,a.OBJECT_NAME,a.OBJECT_TYPE) "Code"
                  from dba_objects a
where exists (select *
                from dba_source x
               where x.owner = a.OWNER
                 and x.name = a.OBJECT_NAME
                 and x.TYPE = a.OBJECT_TYPE
                 and upper(x.TEXT) like '%STOCKMAN%')
                 order by 3,2""") { ResultSet rs ->
        while (rs.next()) {
            String owner = rs.getString("Owner")
            String name = rs.getString("Name")
            String type = rs.getString("Type")
            Clob code = rs.getClob("Code")

            new BufferedReader(code.getCharacterStream()).withReader {
                String line
                Paths.get(System.properties.'user.home','Documents','Files','Work','Utica Backup','Source',"${name} ${type}.txt").withWriter { BufferedWriter bw ->
                    while ((line = it.readLine()) != null) {
                        bw.write(line)
                        bw.newLine()
                    }
                }
            }
        }
    }

}
