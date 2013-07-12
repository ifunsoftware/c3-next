[![Build Status](https://api.travis-ci.org/ifunsoftware/c3-next.png)](https://travis-ci.org/ifunsoftware/c3-next)

C3-next (Content-Communication-Collaboration platform)
----------------------------------------------------------

### Repository

https://github.com/ifunsoftware/c3-next

### Requirements

* java -version  1.7+
* virgo [download virgo] (http://www.eclipse.org/virgo/download/)  -version 3.5+

in .m2/settings.xml :

```bash
<profile>
    <id>development</id>
    <properties>
        <virgo.path.libs>/opt/virgo/repository/usr</virgo.path.libs>
        <virgo.path.pickup>/opt/virgo/pickup</virgo.path.pickup>
    </properties>
</profile>
```

### Compile

```bash
mvn clean install -Pdevelopment
```

### Run

```bash
cd $VIRGO_HOME/virgo/bin/
./startup.sh
```

### C3 management console
admin/password
http://localhost:8080/manage/

### File system endpoint

http://localhost:8080/rest/fs/

### C3 api doc

[docs](http://localhost:8080/rest/static/api.html)
