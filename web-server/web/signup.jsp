<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Sign Up</title>
</head>
<body>
<form action="signup" method="POST">
    <label>
        Email:
        <input type="email" name="email" placeholder="wilsonyoung123@gmail.com" required="required" maxlength="255"/>
    </label>
    <label>
        Username:
        <input type="text" name="username" placeholder="Wilson Young" required="required" maxlength="255"/>
    </label>
    <label>
        Password:
        <input type="password" name="password" placeholder="**************" required="required" maxlength="255"/>
    </label>
    <input type="submit" value="Sign Up"/>
</form>
</body>
</html>
