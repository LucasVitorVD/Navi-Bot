package com.project.navi.quote;

import java.util.Optional;

public interface MotivationalQuoteProvider {

    Optional<Quote> fetch();
}
