/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
import { deleteDownloaded, pauseOrResumeDownload } from "./mlnDownloaderFetch.js";

let downloads = new Map();

document.addEventListener("DOMContentLoaded", () => {
    connect();
    const container = document.getElementById('downloadsContainer');
});

const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws-connect'
});

const SUBSCRIBE_PREFIX = "/topic";

stompClient.onConnect = (frame) => {

    setConnected(true);

    console.log('Connected: ' + frame);

    stompClient.subscribe(`${SUBSCRIBE_PREFIX}/downloads`, (jsonString) => {
        subcriptionDownload(jsonString);
    });

    stompClient.subscribe(`${SUBSCRIBE_PREFIX}/disc_info`, (jsonString) => {

        const discInfo = JSON.parse(jsonString.body)

        const spaceInfo = `${getBytesToGB(discInfo.freeSpace)} / ${getBytesToGB(discInfo.totalSpace)} GB`;

        document.querySelector(`[data-file="disc-info"]`).textContent = spaceInfo;
    });
};

function subcriptionDownload(jsonString) {

    const data = JSON.parse(jsonString.body);

    const downloadsContainer = document.querySelector("#downloadsContainer");


    if (Array.isArray(data) && data.length === 0) {
        downloadsContainer.innerHTML = "";
        downloads = new Map();
    }


    const ids = new Set(data.map(x => `download_${x.id}`));

    for (const key of downloads.keys()) {
        if (!ids.has(key)) {
            downloads.delete(key);
            document.querySelector(`#${key}`).remove();
        }
    }


    data.forEach((download, index) => {

        const downloadOnMap = downloads.get("download_" + download.id)

        if (downloadOnMap) {
            updateCard(download)
        } else {
            downloads.set("download_" + download.id, download);

            downloadsContainer.insertAdjacentHTML(
                "beforeend",
                createDownloadCard(download, index)
            );

            const actionBtn = document.querySelector(`[data-file="actionBtn_${download.id}"]`);
            const delBtn = document.querySelector(`[data-file="delBtn_${download.id}"]`);


            actionBtn.addEventListener("click", (e) => {
                const btn = e.target;
                const id = btn.dataset.file.split("_")[1];
                pauseOrResumeDownload(id)
            })

            delBtn.addEventListener("click", (e) => {
                const id = e.target.dataset.file.split("_")[1];
                deleteDownloaded(id, downloads)
            })
        }
    });

}

function updateCard(download) {

    const actionBtn = document.querySelector(`[data-file="actionBtn_${download.id}"]`)
    const delBtn = document.querySelector(`[data-file="delBtn_${download.id}"]`)

    if (download.isDownloading & !download.isDownloaded) {
        actionBtn.innerHTML = "Pause";
    } else {
        actionBtn.innerHTML = "Resume";
    }

    if (download.isDownloaded) {
        actionBtn.disabled = true;
        delBtn.disabled = true;
    } else {
        actionBtn.disabled = false;
        delBtn.disabled = false;
    }


    const downloadedBytes = document.querySelector(`[data-file="downloadedBytes_${download.id}"]`);

    if (downloadedBytes == null)
        downloadedBytes = 0;

    if (Number(downloadedBytes.dataset.size) != download.downloadedBytes) {
        downloadedBytes.textContent = `${getBytesToMb(download.downloadedBytes)} MB`;
    }


    const status = document.querySelector(`[data-file="status_${download.id}"]`)

    status.innerHTML =
        `<div class="fw-semibold small ${download.isDownloaded ? 'text-success' : 'text-primary'}" data-file="status_${download.id}">
        ${download.isDownloaded ? 'Completed' : (download.isDownloading ? 'Downloading' : 'Paused')}
    </div>`


    const progressNodes = document.querySelectorAll(`[data-file="progress_${download.id}"]`)
    const progress =
        download.length > 0
            ? Math.floor((download.downloadedBytes / download.length) * 100)
            : 0;


    progressNodes.forEach(node => {

        const progressText = `${progress}%`;

        node.textContent = progressText;

        if (node.dataset.type === "bar") {
            node.style.width = progressText;
        }
    });


    const partProgressNodes = document.querySelectorAll(`[data-file="partProgress_${download.id}"]`)


    partProgressNodes.forEach(node => {

        const progressText = `${progress}%`;

        node.textContent = progressText;

        if (node.dataset.type === "bar") {
            node.style.width = progressText;
        }
    });


}

function getBytesToMb(downloadedBytes) {
    return ((downloadedBytes) / 1000 / 1000).toFixed(2);
}

function getBytesToGB(downloadedBytes) {
    return ((downloadedBytes) / 1000 / 1000 / 1000).toFixed(2);
}

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    console.log("CONNECTED: " + connected)
}

function connect() {
    stompClient.activate();
}

function createDownloadCard(download, index) {

    const progress =
        download.length > 0
            ? Math.floor((download.downloadedBytes / download.length) * 100)
            : 0;

    return `

    <div class="card shadow-sm border-0 mb-3 fade show" id="download_${download.id}">

        <div class="card-body py-3">

            <!-- HEADER -->
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">

                <div>
                    <h6 class="card-title mb-0 text-break fw-semibold">
                        ${download.filePath.split('/').pop()}
                    </h6>

                    <small class="text-body-secondary">
                        ${download.filePath}
                    </small>
                </div>

                <!-- actions -->
                <div class="d-flex gap-2">

                    <button
                        class="btn btn-warning btn-sm py-1 px-3 pause-btn"
                        data-index="${index}"
                        data-file="actionBtn_${download.id}">

                        ${download.isDownloading ? 'Pause' : 'Resume'}

                    </button>

                    <button
                        class="btn btn-danger btn-sm py-1 px-3 cancel-btn"
                        data-index="${index}"
                        data-file="delBtn_${download.id}">Delete</button>
                </div>

            </div>

            <!-- INFO -->
            <div class="row mt-3 g-2">

                <div class="col-6 col-lg-3">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Tamaño
                        </small>

                        <div class="fw-semibold small">
                            ${(download.length / 1000 / 1000).toFixed(2)} MB
                        </div>

                    </div>
                </div>

                <div class="col-6 col-lg-3">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Descargado
                        </small>

                        <div class="fw-semibold small" data-file="downloadedBytes_${download.id}" data-size="${download.downloadedBytes}">
                            ${(download.downloadedBytes / 1000 / 1000).toFixed(2)} MB
                        </div>

                    </div>
                </div>

                <div class="col-6 col-lg-3">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Fragmentos
                        </small>

                        <div class="fw-semibold small">
                            ${download.chunks}
                        </div>

                    </div>
                </div>

                <div class="col-6 col-lg-3">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Estado
                        </small>

                        <div class="fw-semibold small ${download.isDownloaded ? 'text-success' : 'text-primary'}" data-file="status_${download.id}">

                            ${download.isDownloaded ? 'Completed' : (download.isDownloading ? 'Downloading' : 'Paused')}

                        </div>

                    </div>
                </div>

            </div>

            <!-- TOTAL PROGRESS -->
            <div class="mt-3">

                <div class="d-flex justify-content-between mb-1">

                    <small class="fw-semibold">
                        Progreso total
                    </small>

                    <small data-file="progress_${download.id}" data-type="text">
                        ${progress}%
                        
                    </small>

                </div>

                <div class="progress"
                    style="height: 16px;">

                    <div class="progress-bar progress-bar-striped progress-bar-animated"
                        role="progressbar"
                        style="width: ${progress}%"
                        data-file="progress_${download.id}"
                        data-type="bar">

                        ${progress}%

                    </div>

                </div>

            </div>

            <!-- PARTS -->
            <div class="accordion accordion-flush mt-3"
                id="downloadAccordion_${index}">

                <div class="accordion-item">

                    <h2 class="accordion-header">

                        <button class="accordion-button collapsed py-2"
                            type="button"
                            data-bs-toggle="collapse"
                            data-bs-target="#collapseParts_${index}">

                            Fragmentos de descarga

                        </button>

                    </h2>

                    <div id="collapseParts_${index}"
                        class="accordion-collapse collapse"
                        data-bs-parent="#downloadAccordion_${index}">

                        <div class="accordion-body p-0">

                            ${download.parts.map((part, partIndex) => {

        const partProgress =
            part.length > 0
                ? Math.floor((part.actualSize / part.length) * 100)
                : 0;

        return `

                                <div class="border-bottom p-2">

                                    <div class="d-flex justify-content-between align-items-center mb-1">

                                        <div>

                                            <div class="fw-semibold small">
                                                PART_${partIndex}
                                            </div>

                                            <small class="text-body-secondary">
                                                ${(part.length / 1000 / 1000).toFixed(2)} MB
                                            </small>

                                        </div>

                                        <span class="badge text-bg-primary" data-file="partProgress_${download.id}" data-type="text">
                                            ${partProgress}%
                                        </span>

                                    </div>

                                    <div class="progress"
                                        style="height: 12px;">

                                        <div class="progress-bar"
                                            role="progressbar"
                                            style="width: ${partProgress}%"
                                             data-file="partProgress_${download.id}"
                                            data-type="bar">

                                        </div>

                                    </div>

                                </div>

                                `;
    }).join('')}

                        </div>

                    </div>

                </div>

            </div>

        </div>

    </div>

    `;
}