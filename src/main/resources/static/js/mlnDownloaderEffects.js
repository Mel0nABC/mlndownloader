/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */

document.addEventListener("DOMContentLoaded", () => {
    const body = document.querySelector("body");
    body.classList.remove("d-none");
    setTimeout(() => {
        body.classList.add("show");
    }, 150)
})