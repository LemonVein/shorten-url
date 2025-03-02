<!DOCTYPE html>
<html>
<head>
    <title>register</title>
    <link rel="stylesheet" href="/css/login.css" />
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
</head>
<body>
<h1>register Page</h1>
<form onsubmit="register(event)">
    <label for="username">Username:</label>
    <input type="text" id="username" name="username" required>
    <br>
    <label for="password">Password:</label>
    <input type="password" id="password" name="password" required>
    <br>
    <label for="confirmPassword">Confirm Password:</label>
    <input type="password" id="confirmPassword" name="confirmPassword" required>
    <button type="submit">register</button>
</form>
<a href="/main">to main</a>
<script src="/js/register.js"></script>
</body>
</html>