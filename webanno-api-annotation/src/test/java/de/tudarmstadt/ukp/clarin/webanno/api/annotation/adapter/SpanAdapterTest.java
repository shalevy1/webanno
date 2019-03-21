/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class SpanAdapterTest
{
    private FeatureSupportRegistry featureSupportRegistry;
    private Project project;
    private AnnotationLayer neLayer;
    private JCas jcas;
    private SourceDocument document;
    private String username;
    private List<SpanLayerBehavior> behaviors;

    @Before
    public void setup() throws Exception
    {
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }
        
        username = "user";
        
        project = new Project();
        project.setId(1l);
        project.setMode(PROJECT_TYPE_ANNOTATION);
        
        document = new SourceDocument();
        document.setId(1l);
        document.setProject(project);
        
        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "NE", SPAN_TYPE, project, true,
                TOKENS);
        neLayer.setId(1l);

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList());
        
        behaviors = asList(new SpanStackingBehavior(), new SpanCrossSentenceBehavior(),
                new SpanAnchoringModeBehavior());
    }
    
    @Test
    public void thatSpanCrossSentenceBehaviorOnCreateThrowsException()
    {
        neLayer.setCrossSentence(false);
        
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        SpanAdapter sut = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                behaviors);

        assertThatExceptionOfType(MultipleSentenceCoveredException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 
                        jcas.getDocumentText().length()))
                .withMessageContaining("covers multiple sentences");
    }

    @Test
    public void thatSpanCrossSentenceBehaviorOnValidateReturnsErrorMessage()
        throws AnnotationException
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        SpanAdapter sut = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                behaviors);

        // Add two annotations
        neLayer.setCrossSentence(true);
        sut.add(document, username, jcas.getCas(), 0, jcas.getDocumentText().length());
        
        //Validation fails
        neLayer.setCrossSentence(false);
        assertThat(sut.validate(jcas.getCas()))
                .extracting(Pair::getLeft)
                .usingElementComparatorIgnoringFields("source", "message")
                .containsExactly(LogMessage.error(null, ""));
    }
    
    @Test
    public void thatSpanStackingBehaviorOnCreateThrowsException() throws AnnotationException
    {
        neLayer.setAllowStacking(false);
        
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        SpanAdapter sut = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                behaviors);

        // First time should work
        sut.add(document, username, jcas.getCas(), 0, 1);
        
        // Second time not
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .withMessageContaining("stacking is not enabled");
    }

    @Test
    public void thatSpanStackingBehaviorOnValidateGeneratesErrors() throws AnnotationException
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        SpanAdapter sut = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                behaviors);

        // Add two annotations
        neLayer.setAllowStacking(true);
        sut.add(document, username, jcas.getCas(), 0, 1);
        sut.add(document, username, jcas.getCas(), 0, 1);
        
        //Validation fails
        neLayer.setAllowStacking(false);
        assertThat(sut.validate(jcas.getCas()))
                .extracting(Pair::getLeft)
                .usingElementComparatorIgnoringFields("source", "message")
                .containsExactly(LogMessage.error(null, ""));
    }

    @Test
    public void thatSpanAnchoringAndStackingBehaviorsWorkInConcert() throws AnnotationException
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        SpanAdapter sut = new SpanAdapter(featureSupportRegistry, null, neLayer, asList(),
                behaviors);

        // First time should work - we annotate the whole word "This"
        sut.add(document, username, jcas.getCas(), 0, 4);
        
        // Second time not - here we annotate "T" but it should be expanded to "This"
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .withMessageContaining("stacking is not enabled");
    }
}
