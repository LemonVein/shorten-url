<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Short URL</title>
    <link rel="stylesheet" href="/css/main.css" />
</head>
<body>
<div class="top-nav">
    <p class="username" id="usernameDisplay" style="display: none;"></p>
    <form id="logoutForm" action="/logout" method="POST" style="display: none;">
        <button type="submit" class="login-button">Logout</button>
    </form>
    <a href="/register" id="registerButton" class="login-button" style="display: none">register</a>
    <a href="/login" id="loginButton" class="login-button" style="display: none;">login</a>
</div>
<header>
    <h1>Create a Shorten URL</h1>
</header>
<main>
    <form id="urlForm">
        <label for="originalUrl">Original URL:</label>
        <input id="originalUrl" name="originalUrl" required placeholder="https://example.com">
        <button type="submit">Shorten</button>
    </form>
    <div class="result" id="result">
        <a></a>
    </div>
    <section id="urlListSection" style="display: none;">
        <h2>Created Shorten URL</h2>
        <ul id="urlList"></ul>
    </section>
</main>
<script src="/js/main.js"></script>
</body>
</html>