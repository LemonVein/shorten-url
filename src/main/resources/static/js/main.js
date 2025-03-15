const loginButton = document.querySelector("#loginButton");
const logoutButton = document.querySelector("#logoutForm");
const urlListSection = document.querySelector("#urlListSection");
const registerButton = document.querySelector("#registerButton");
const userNameDisplay = document.querySelector('#usernameDisplay');

const mainDomain = "http://localhost:8080/"
let currentPage = 1;
const itemsPerPage = 5;
let urlData = [];

document.addEventListener("DOMContentLoaded", async () => {
    const resultA = document.querySelector('div.result > a');
    const form = document.querySelector("#urlForm");
    const token = localStorage.getItem("token");
    const name = document.querySelector(".username");

    await checkAuthStatus();

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
                const shortenedUrl = mainDomain + `${shortenCode}`;
                alert(`Shortened URL: ${shortenedUrl}`);

                resultA.href = `/${shortenCode}`;
                resultA.textContent = shortenedUrl;
                resultA.target = "_blank";
                originalUrl.value = "";

                if (token) {
                    await fetchUserUrls();
                }
            } else {
                alert("Failed to create shortened URL");
            }
        } catch (error) {
            alert("Error: " + error.message);
        }
    });

    document.getElementById("logoutForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            const response = await fetch("/auth/logout", {
                method: "POST",
                credentials: "include"  // 쿠키 포함 (리프레시 토큰 삭제)
            });

            if (response.ok) {
                localStorage.removeItem("token");

                const nameElement = document.getElementById("name");
                if (nameElement) {
                    nameElement.textContent = "";
                    nameElement.style.display = "none";
                }

                window.location.href = "/main";
            } else {
                console.error("로그아웃 실패:", response.status);
            }
        } catch (error) {
            console.error("로그아웃 요청 중 오류 발생:", error);
        }
    });
});

async function fetchUserUrls() {
    try {
        const response = await fetchWithToken("/api/my-urls", {
            method: "GET",
            headers: { "Content-Type": "application/x-www-form-urlencoded" }
        });

        if (!response.ok) {
            throw new Error("URL 목록을 불러올 수 없습니다.");
        }

        const urls = await response.json();
        urlData = urls.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)); // 최신순 정렬
        currentPage = 1; // 페이지 초기화
        renderPage(); // 페이지 렌더링

    } catch (error) {
        console.error("Error fetching URLs:", error);
    }
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
                              <a href="/${url.shortUrl}" target="_blank">${mainDomain}${url.shortUrl}</a>
                              <span class="url-status" data-url="${url.originalUrl}">⏳</span>
                              <button class="check-url-btn" data-url="${url.originalUrl}">Check</button>
                              <button class="delete-btn" data-shorturl="${url.shortUrl}">Delete</button>
        `;
        urlList.appendChild(listItem);
    });

    document.querySelectorAll(".delete-btn").forEach(button => {
        button.addEventListener("click", async (event) => {
            const shortUrl = event.target.getAttribute("data-shorturl");
            await deleteUrl(shortUrl);
        });
    });

    document.querySelectorAll(".check-url-btn").forEach(button => {
        button.addEventListener("click", async (event) => {
            const originalUrl = event.target.getAttribute("data-url");
            await checkSingleUrl(originalUrl);
        });
    });

    if (currentPage === 1) {
        checkOriginalUrls();
    }

    createPaginationButtons();
}

async function deleteUrl(shortUrl) {
    try {
        const response = await fetchWithToken(`/api/${shortUrl}`, {
            method: "DELETE"
        });

        if (response.ok) {
            alert("URL deleted successfully!");
            fetchUserUrls(); // 삭제 후 목록 다시 불러오기
        } else {
            alert("Failed to delete URL.");
            fetchUserUrls();
        }
    } catch (error) {
        console.error("Error deleting URL:", error);
        alert("An error occurred while deleting the URL.");
    }
}

function createPaginationButtons() {
    const pagination = document.getElementById("pagination");
    const totalPages = Math.ceil(urlData.length / itemsPerPage);

    if (totalPages <= 1) return;

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

// 원시적인 로그인 버튼 끄고 켜기
function turnOffLogin() {
    loginButton.style.display = "block";
    registerButton.style.display = "block";
    logoutButton.style.display = "none";
    urlListSection.style.display = "none";
    userNameDisplay.style.display = "none";
}

function turnOnLogin() {
    loginButton.style.display = "none";
    registerButton.style.display = "none";
    logoutButton.style.display = "block";
    urlListSection.style.display = "block";
    userNameDisplay.style.display = "block";
}

async function fetchWithToken(url, options = {}) {
    let token = localStorage.getItem("token");

    if (!options.headers) {
        options.headers = {};
    }
    options.headers["Authorization"] = `Bearer ${token}`;

    console.log("Request URL:", url);
    console.log("Request Options:", options);

    let response = await fetch(url, options);

    if (response.status === 401) { // 액세스 토큰이 만료되었을 경우
        console.log("액세스 토큰이 만료됨. 리프레시 요청");

        const refreshResponse = await fetch("/auth/refresh", {
            method: "POST",
            credentials: "include"
        });

        if (refreshResponse.ok) {
            const data = await refreshResponse.json();
            localStorage.setItem("token", data.token); // 새로운 액세스 토큰 저장

            // 원래 요청을 새로운 토큰으로 다시 실행
            options.headers["Authorization"] = `Bearer ${data.token}`;
            return fetch(url, options);
        } else {
            console.warn("리프레시 토큰도 만료됨. 다시 로그인 필요.");
            localStorage.removeItem("token"); // 만료된 토큰 삭제
        }
    }

    return response;
}

async function checkAuthStatus() {
    let token = localStorage.getItem("token");

    if (!token) {
        await refreshToken();
        token = localStorage.getItem("token");
    }

    if (!token) {
        console.log("로그인되지 않은 사용자입니다.");
        turnOffLogin();
        return;
    }

    try {
        const response = await fetch("/api/status", {
            method: "GET",
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (response.ok) {
            const data = await response.json();
            console.log("인증된 사용자:", data.username);
            document.querySelector(".username").textContent = `Hello! ${data.username}`;
            turnOnLogin();
            fetchUserUrls();
        } else if (response.status === 401 || response.status === 403) {
            console.warn("토큰 만료됨. 리프레시 시도");
            await refreshToken(); // 🔥 리프레시 요청
        } else {
            console.warn("인증 상태 확인 실패:", response.status);
            turnOffLogin();
        }
    } catch (error) {
        console.error("인증 상태 요청 중 오류 발생:", error);
        turnOffLogin();
    }
}

async function refreshToken() {
    try {
        const refreshResponse = await fetch("/auth/refresh", {
            method: "POST",
            credentials: "include" // HttpOnly 쿠키에서 리프레시 토큰 요청
        });

        if (refreshResponse.ok) {
            const data = await refreshResponse.json();
            localStorage.setItem("token", data.token); // 새 액세스 토큰 저장
            console.log("리프레시 성공. 새로운 토큰 발급됨.");

            await checkAuthStatus();
        } else {
            console.warn("리프레시 토큰도 만료됨. 로그인 필요.");
            localStorage.removeItem("token");
        }
    } catch (error) {
        console.error("리프레시 요청 중 오류 발생:", error);
        localStorage.removeItem("token");
    }
}

async function checkOriginalUrls() {
    document.querySelectorAll(".url-status").forEach(async (statusSpan) => {
        const originalUrl = statusSpan.getAttribute("data-url");
        const isValid = await checkUrlValidity(originalUrl);
        statusSpan.textContent = isValid ? "✅" : "❌";
    });
}

async function checkSingleUrl(originalUrl) {
    const isValid = await checkUrlValidity(originalUrl);
    const statusSpan = document.querySelector(`.url-status[data-url="${originalUrl}"]`);
    if (statusSpan) {
        statusSpan.textContent = isValid ? "✅" : "❌";
    }
}

async function checkUrlValidity(originalUrl) {
    try {
        const response = await fetchWithToken(`/api/state?originalUrl=${encodeURIComponent(originalUrl)}`, {
            method: "GET"
        });

        if (response.ok) {
            const data = await response.json();
            return data.isValid;  // 백엔드에서 { "isValid": true } 또는 { "isValid": false } 반환
        } else {
            return false;
        }
    } catch (error) {
        console.error("URL 유효성 검사 중 오류 발생:", error);
        return false;
    }
}