package com.knowledge.base.config;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CustomLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {

    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer("standard")
            .custom()
            .tokenizer("standard")
            .tokenFilter("lowercase")
            .tokenFilter("asciifolding");

        context.analyzer("chinese")
            .custom()
            .tokenizer("standard")
            .tokenFilter("lowercase")
            .tokenFilter("asciifolding")
            .tokenFilter("cjk_bigram");

        context.analyzer("text")
            .custom()
            .tokenizer("standard")
            .tokenFilter("lowercase")
            .tokenFilter("asciifolding")
            .tokenFilter("stop")
            .tokenFilter("porterstem");

        context.normalizer("lowercase")
            .custom()
            .tokenFilter("lowercase")
            .tokenFilter("asciifolding");
    }
}
