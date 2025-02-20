const loginButton = document.querySelector("#loginButton");
const logoutButton = document.querySelector("#logoutForm");
const urlListSection = document.querySelector("#urlListSection");
const registerButton = document.querySelector("#registerButton");

document.addEventListener("DOMContentLoaded", async () => {
    const resultA = document.querySelector('div.result > a');
    const form = document.querySelector("#urlForm");
    const token = localStorage.getItem("token");

    const name = document.querySelector(".username");

    turnOffLogin();

    if (token) {
        try {
            const response = await fetch("/status", {
                method: "GET",
                headers: { "Authorization": `Bearer ${token}` }
            });

            if (response.ok) {
                const data = await response.json();
                console.log("📌 사용자 정보:", data);

                // 로그인된 경우 UI 업데이트
                name.textContent = `Hello! ${data.username}`;
                name.style.display = "block";

                turnOnLogin();

                fetchUserUrls(); // URL 목록 불러오기
            } else {
                console.warn("🚨 사용자 정보를 가져올 수 없음.");
            }
        } catch (error) {
            console.error("🚨 사용자 정보 요청 중 오류 발생:", error);
        }
    } else {
        turnOffLogin();
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const originalUrl = document.getElementById("originalUrl").value;

        const formData = new URLSearchParams();
        formData.append("originalUrl", originalUrl);

        try {
            const token = localStorage.getItem("token")

            const response = await fetch("/shorten_url", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded",
                           "Authorization": token ? `Bearer ${token}` : ""},
                body: formData.toString()
            });

            if (response.ok) {
                const shortenCode = await response.text();
                const shortenedUrl = `http://localhost:8080/connect/${shortenCode}`;
                alert(`Shortened URL: ${shortenedUrl}`);

                resultA.href = shortenedUrl;
                resultA.textContent = shortenedUrl;
                resultA.target = "_blank"

                if (token) {
                    fetchUserUrls();
                }
            } else {
                alert("Failed to create shortened URL");
            }
        } catch (error) {
            alert("Error: " + error.message);
        }
    });
    document.getElementById("logoutForm").addEventListener("submit", (event) => {
        event.preventDefault(); // 기본 폼 제출 방지
        localStorage.removeItem("token"); // 저장된 토큰 삭제
        name.textContent = ``;
        name.style.display = "none";
        window.location.href = "/main";
    });
});

function fetchUserUrls() {
    const token = localStorage.getItem("token")
    fetch("/my-urls", {
        method: "GET",
        headers: { "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": token ? `Bearer ${token}` : ""},
    }) // 서버에서 사용자의 URL 목록 가져오기
        .then(response => {
            if (!response.ok) {
                throw new Error("URL 목록을 불러올 수 없습니다.");
            }
            return response.json();
        })
        .then(urls => {
            const urlList = document.getElementById("urlList");
            urlList.innerHTML = ""; // 기존 목록 초기화

            if (urls.length === 0) {
                urlList.innerHTML = "<li>No shorten URL was created.</li>";
                return;
            }

            urls.forEach(url => {
                const listItem = document.createElement("li");
                listItem.innerHTML = `<p>original URL : ${url.originalUrl} </p><a href="/connect/${url.shortUrl}" target="_blank">localhost:8080/connect/${url.shortUrl}</a>`;
                urlList.appendChild(listItem);
            });
        })
        .catch(error => {
            console.error("Error:", error);
        });
}

function turnOffLogin() {
    loginButton.style.display = "block";
    registerButton.style.display = "block";
    logoutButton.style.display = "none";
    urlListSection.style.display = "none";
}

function turnOnLogin() {
    loginButton.style.display = "none";
    registerButton.style.display = "none";
    logoutButton.style.display = "block";
    urlListSection.style.display = "block";
}