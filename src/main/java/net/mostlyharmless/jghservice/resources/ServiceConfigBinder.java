/*
 * Copyright 2014 Brian Roach <roach at mostlyharmless dot net>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.mostlyharmless.jghservice.resources;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class ServiceConfigBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        try
        {
            String configFile = InitialContext.doLookup("java:comp/env/configurationPath");
            JAXBContext jaxbContext = JAXBContext.newInstance(ServiceConfig.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            FileReader reader = new FileReader(configFile);
            ServiceConfig c = (ServiceConfig) jaxbUnmarshaller.unmarshal(reader);
            bind(c).to(ServiceConfig.class);
        }
        catch (NamingException ex)
        {
            Logger.getLogger(ServiceConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (JAXBException | FileNotFoundException ex)
        {
            Logger.getLogger(ServiceConfigBinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
