document.addEventListener("DOMContentLoaded", () => {
    const body = document.querySelector("body");
    body.classList.remove("d-none");
    setTimeout(() => {
        body.classList.add("show");
    }, 150)
})