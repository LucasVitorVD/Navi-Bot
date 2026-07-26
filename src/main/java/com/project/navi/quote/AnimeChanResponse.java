package com.project.navi.quote;

record AnimeChanResponse(String status, Data data) {

    record Data(String content, Anime anime, Character character) {
    }

    record Anime(String name) {
    }

    record Character(String name) {
    }
}
