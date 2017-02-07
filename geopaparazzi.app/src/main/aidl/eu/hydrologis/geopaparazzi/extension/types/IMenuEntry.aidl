// IMenuEntry.aidl
package eu.hydrologis.geopaparazzi.extension.types;

/**
 * MenuEntry extension type. An extension that returns a label and icon
 */
interface IMenuEntry {
    /**
     * Returns the text to show in the menu entry
     */
    String getLabel();

    /**
     * Returns the icon
     */
    byte[] getIcon();
}
