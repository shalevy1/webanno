/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.config;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;

import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;

public class JQueryUIResourceBehavior
    extends Behavior
{
    private static final long serialVersionUID = -41338584738835064L;
    
    private static final JQueryUIResourceBehavior INSTANCE = 
            new JQueryUIResourceBehavior();

    public static JQueryUIResourceBehavior get()
    {
        return INSTANCE;
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        // We also load the JQuery CSS always just to get a consistent look across
        // the app
        JQueryUILibrarySettings jqueryCfg = JQueryUILibrarySettings.get();

        if (jqueryCfg.getStyleSheetReference() != null) {
            aResponse.render(
                    CssHeaderItem.forReference(jqueryCfg.getStyleSheetReference()));
        }
    }
}
