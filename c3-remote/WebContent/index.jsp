<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html
        PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title>Upload resource</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>

<form action="/c3-remote/resource/" method="post" enctype="multipart/form-data" accept-charset="utf-8">
    <table>
        <tr>
            <td>Data:</td>
            <td><input type="file" name="data"/></td>
        </tr>
        <tr>
            <td>Metadata mdkey0:</td>
            <td><input type="text" name="mdkey0" value=""/></td>
        </tr>
        <tr>
            <td>User:</td>
            <td><input type="text" name="c3.username" value=""/></td>
        </tr>
        <tr>
            <td>Password:</td>
            <td><input type="password" name="c3.password" value=""/></td>
        </tr>

    </table>
    <br/>
    <input type="submit" value="submit"/>
</form>
</body>
</html>