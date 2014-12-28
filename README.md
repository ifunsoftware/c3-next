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
* URL: http://localhost:8080/manage/
* Default credentials: admin/password


### File system endpoint

http://localhost:8080/rest/fs/

### C3 api doc

[docs](http://localhost:8080/rest/static/api.html)

### Docker Support

After C3 is built with `mvn install` you can immediately build & run a Docker image with new C3 binaries:

```
mvn clean install
cd c3-deploy/target/docker/ && docker build -t ifunsoftware/c3-next:snapshot .
docker run -p 8080:8080 -p 7375:7375 -p 8443:8443 -p 8022:22 c3-next
```

or you can just run the script:

```
./docker_build_and_start.sh
```

This script does the following:

1. Builds C3 artifacts.
2. Stops all currently running containers with latest c3-next image.
3. Builds Docker image with new artifacts.
4. Starts new container.

See https://github.com/ifunsoftware/c3-next-docker repository for more details about C3 Docker support.
