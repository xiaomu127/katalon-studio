/*
 * Copyright 2017 SideeX committers
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

var master = {};
var clickEnabled = true;

function openPanel(tab) {

    let contentWindowId = tab.windowId;
    if (master[contentWindowId] != undefined) {
        chrome.windows.update(master[contentWindowId], {
            focused: true
        }).catch(function(e) {
            master[contentWindowId] == undefined;
            openPanel(tab);
        });
        return;
    } else if (!clickEnabled) {
        return;
    }

    clickEnabled = false;
    setTimeout(function() {
        clickEnabled = true;
    }, 1000);

    var f = function(height, width) {
    chrome.windows.create({
        url: chrome.runtime.getURL("panel/index.html"),
        type: "popup",
        height: height,
        width: width
    }).then(function waitForPanelLoaded(panelWindowInfo) {
        return new Promise(function(resolve, reject) {
            let count = 0;
            let interval = setInterval(function() {
                if (count > 100) {
                    reject("SideeX editor has no response");
                    clearInterval(interval);
                }

                chrome.tabs.query({
                    active: true,
                    windowId: panelWindowInfo.id,
                    status: "complete"
                }).then(function(tabs) {
                    if (tabs.length != 1) {
                        count++;
                        return;
                    } else {
                        master[contentWindowId] = panelWindowInfo.id;
                        if (Object.keys(master).length === 1) {
                            createMenus();
                        }
                        resolve(panelWindowInfo);
                        clearInterval(interval);
                    }
                })
            }, 200);
        });
    }).then(function bridge(panelWindowInfo){
        return chrome.tabs.sendMessage(panelWindowInfo.tabs[0].id, {
            selfWindowId: panelWindowInfo.id,
            commWindowId: contentWindowId
        });
    }).catch(function(e) {
        console.log(e);
    });
    };

    getWindowSize(f);
}

chrome.browserAction.onClicked.addListener(openPanel);

chrome.windows.onRemoved.addListener(function(windowId) {
    let keys = Object.keys(master);
    for (let key of keys) {
        if (master[key] === windowId) {
            delete master[key];
            if (keys.length === 1) {
                chrome.contextMenus.removeAll();
            }
        }
    }
});

function createMenus() {
    chrome.contextMenus.create({
        id: "verifyText",
        title: "verifyText",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "verifyTitle",
        title: "verifyTitle",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "verifyValue",
        title: "verifyValue",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "assertText",
        title: "assertText",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "assertTitle",
        title: "assertTitle",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "assertValue",
        title: "assertValue",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "storeText",
        title: "storeText",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "storeTitle",
        title: "storeTitle",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
    chrome.contextMenus.create({
        id: "storeValue",
        title: "storeValue",
        documentUrlPatterns: ["<all_urls>"],
        contexts: ["all"]
    });
}

var port;
chrome.contextMenus.onClicked.addListener(function(info, tab) {
    port.postMessage({ cmd: info.menuItemId });
});

chrome.runtime.onConnect.addListener(function(m) {
    port = m;
});

/* KAT-BEGIN remove showing sideex docs
chrome.runtime.onInstalled.addListener(function(details) {
    if (details.reason == "install" || details.reason == "update") {
        chrome.tabs.create({url: "http://sideex.org"});
    }
})
KAT-END */