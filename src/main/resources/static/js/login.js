async function login(event) {
    event.preventDefault(); // 기본 폼 제출 방지

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const response = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            username: username,
            password: password
        })
    });

    if (response.ok) {
        const data = await response.json();
        localStorage.setItem("token", data.token);
        window.location.href = "/main"; // 로그인 후 메인 페이지 이동
    } else {
        alert("Invalid username or password.");
    }
}