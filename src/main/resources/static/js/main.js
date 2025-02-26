const loginButton = document.querySelector("#loginButton");
const logoutButton = document.querySelector("#logoutForm");
const urlListSection = document.querySelector("#urlListSection");
const registerButton = document.querySelector("#registerButton");

let currentPage = 1;
const itemsPerPage = 5;
let urlData = [];

document.addEventListener("DOMContentLoaded", async () => {
    const resultA = document.querySelector('div.result > a');
    const form = document.querySelector("#urlForm");
    const token = localStorage.getItem("token");
    const name = document.querySelector(".username");

    turnOffLogin();

    if (token) {
        try {
            const response = await fetch("/api/status", {
                method: "GET",
                headers: { "Authorization": `Bearer ${token}` }
            });

            if (response.ok) {
                const data = await response.json();
                console.log("📌 사용자 정보:", data);

                name.textContent = `Hello! ${data.username}`;
                name.style.display = "block";

                turnOnLogin();
                fetchUserUrls();
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
        let originalUrl = document.getElementById("originalUrl");

        const formData = new URLSearchParams();
        formData.append("originalUrl", originalUrl.value);

        try {
            const token = localStorage.getItem("token")

            const response = await fetch("/api/shorten_url", {
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
                resultA.target = "_blank";
                originalUrl.value = "";

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
        event.preventDefault();
        localStorage.removeItem("token");
        name.textContent = ``;
        name.style.display = "none";
        window.location.href = "/main";
    });
});

function fetchUserUrls() {
    const token = localStorage.getItem("token")
    fetch("/api/my-urls", {
        method: "GET",
        headers: { "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": token ? `Bearer ${token}` : ""},
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("URL 목록을 불러올 수 없습니다.");
            }
            return response.json();
        })
        .then(urls => {
            urlData = urls; // 전체 데이터 저장
            currentPage = 1; // 페이지 초기화
            renderPage(); // 페이지 렌더링
        })
        .catch(error => {
            console.error("Error:", error);
        });
}

function renderPage() {
    const urlList = document.getElementById("urlList");
    const pagination = document.getElementById("pagination");
    urlList.innerHTML = "";
    pagination.innerHTML = "";

    const start = (currentPage - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    const paginatedData = urlData.slice(start, end);

    if (paginatedData.length === 0) {
        urlList.innerHTML = "<li>No shorten URL was created.</li>";
        return;
    }

    paginatedData.forEach(url => {
        const listItem = document.createElement("li");
        listItem.innerHTML = `<p>original URL : ${url.originalUrl} </p>
                              <a href="/connect/${url.shortUrl}" target="_blank">localhost:8080/connect/${url.shortUrl}</a>`;
        urlList.appendChild(listItem);
    });

    createPaginationButtons();
}

function createPaginationButtons() {
    const pagination = document.getElementById("pagination");
    const totalPages = Math.ceil(urlData.length / itemsPerPage);

    if (totalPages <= 1) return; // 페이지가 하나면 버튼 필요 없음

    // 이전 페이지 버튼
    if (currentPage > 1) {
        const prevButton = document.createElement("button");
        prevButton.textContent = "«";
        prevButton.onclick = () => changePage(currentPage - 1);
        pagination.appendChild(prevButton);
    }

    // 페이지 번호 버튼
    for (let i = 1; i <= totalPages; i++) {
        const pageButton = document.createElement("button");
        pageButton.textContent = i;
        if (i === currentPage) {
            pageButton.style.fontWeight = "bold";
        }
        pageButton.onclick = () => changePage(i);
        pagination.appendChild(pageButton);
    }

    // 다음 페이지 버튼
    if (currentPage < totalPages) {
        const nextButton = document.createElement("button");
        nextButton.textContent = "»";
        nextButton.onclick = () => changePage(currentPage + 1);
        pagination.appendChild(nextButton);
    }
}

function changePage(page) {
    currentPage = page;
    renderPage();
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