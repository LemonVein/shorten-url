<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
    <link rel="stylesheet" href="/css/login.css" />
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
</head>
<body>
<h1>Sign in to Shorten Url</h1>
<form onsubmit="login(event)">
    <label for="username">Username:</label>
    <input type="text" id="username" name="username" required>
    <br>
    <label for="password">Password:</label>
    <input type="password" id="password" name="password" required>
    <br>
    <button type="submit">Login</button>
</form>
<a href="/main">to main</a>
<script src="/js/login.js"></script>
</body>
</html>