"use strict";
function getScrollHeight() {
    return document.documentElement.scrollHeight
}

function getScreenHeight() {
    return window.innerHeight
}

function getCurrentScrollHeight() {
    return document.documentElement.scrollTop
}

function calcPageCount() {
    return Math.round(getScrollHeight() / getScreenHeight() - 0.5)
}

function scrollAbsY(y) {
    window.scrollTo(0, y)
}

function getBodyPaddingTop() {
    var body = document.getElementsByTagName('body')[0];
    return window.getComputedStyle(body, null).getPropertyValue('padding-top')
}

function getBodyPaddingBottom() {
    var body = document.getElementsByTagName('body')[0];
    return window.getComputedStyle(body, null).getPropertyValue('padding-bottom')
}

function scrollByOffset(offset) {
    var paddingTop = getBodyPaddingTop()
    var paddingBottom = getBodyPaddingBottom()
    var amount = 0

    if (offset == 0) {
        amount = parseInt(paddingTop, 10)
    } else if (offset == calcPageCount() - 1) {
        amount = getScrollHeight() - getScreenHeight() - parseInt(paddingBottom)
    } else {
        amount = offset * getScreenHeight() + parseInt(paddingTop)
    }
    window.scrollTo(0, amount)
}

function scrollToAnchor(anchor) {
    var top = document.getElementById(anchor).offsetTop; //Getting Y of target element
    window.scrollTo(0, top);
    //location.hash = "#" + anchor;
}
