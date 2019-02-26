package org.mb.tedd.statement;

import java.util.Arrays;
import java.util.stream.Collectors;

public class LocatorContainerNames {

    private enum LocatorContainerName {
        FIND_ELEMENT ("findElement"),
        FIND_ELEMENTS ("findElements");

        private String locatorContainerName;

        LocatorContainerName(String locatorContainerName){
            this.locatorContainerName = locatorContainerName;
        }

        public String getLocatorContainerName(){
            return this.locatorContainerName;
        }
    }

    public static boolean isLocatorContainer(String strategy){
        return Arrays.stream(LocatorContainerName.values())
                .map(LocatorContainerName::getLocatorContainerName)
                .collect(Collectors.toList())
                .contains(strategy);
    }
}
