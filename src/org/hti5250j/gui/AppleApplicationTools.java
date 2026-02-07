package org.hti5250j.gui;

import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import java.awt.*;
import java.util.List;

import static java.lang.Class.forName;

class AppleApplicationTools {

    private HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    boolean tryToSetDockIconImages(List<Image> images) {
        return tryToSetDockIconImage(images.get(images.size() - 1));
    }

    private boolean tryToSetDockIconImage(Image image) {
        if (isAppleEnvironment()) {
            try {
                Class applicationClass = forName("com.apple.eawt.Application");
                Object application = applicationClass.getMethod("getApplication").invoke(applicationClass);
                applicationClass.getMethod("setDockIconImage", Image.class).invoke(application, image);
                return true;
            } catch (Exception e) {
                log.debug("Skipping to set application dock icon for Mac OS X, because didn't found 'com.apple.eawt.Application' class.", e);
            }
        }
        return false;
    }

    private boolean isAppleEnvironment() {
        try {
            forName("com.apple.eawt.Application");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
