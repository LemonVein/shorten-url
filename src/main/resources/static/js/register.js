async function register(event) {
    event.preventDefault(); // 기본 폼 제출 방지

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    if (password !== confirmPassword) {
        alert("Password and confirm password do not match.")
        return
    }

    const response = await fetch("/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            username: username,
            password: password
        })
    });

    if (response.ok) {
        const data = await response.json();
        alert("registered ok")
        window.location.href = "/main"; // 로그인 후 메인 페이지 이동
    } else {
        alert("Invalid username or password.");
    }
}