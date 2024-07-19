# com.marginallyclever.interactivecomponentlist.InteractiveComponentList

InteractiveComponentList is a Java Swing component that offers a vertical list of interactive 
components, which can be sorted through drag-and-drop functionality. Unlike traditional JList 
or JTable components, InteractiveComponentList allows for the components within the list to 
remain interactive, providing a more dynamic and flexible user experience.

## Features

- **Drag-and-Drop Sorting**: Easily reorder components within the list by dragging and dropping.
- **Auto-Scrolling**: Supports auto-scrolling when dragging elements near the top or bottom of the viewport, enhancing the user experience during reordering.
- **Interactive Components**: Unlike JList, components within the list remain interactive, allowing for a wide range of component types to be included in the list.
- **ListDataListener Support**: Subscribe to ListDataListener events to be notified when the list changes order.

## Usage

add new Components to an InteractiveComponentList with `add(Component c)` and `remove(Component c)`.  
ListDataListener subscribers will be notified when the list changes order or an item is deleted.

```java
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        JFrame frame = new JFrame();
        InteractiveComponentList panel = new InteractiveComponentList();

        panel.add(new JButton("Item 1"));
        panel.add(new JButton("Item 2"));
        panel.add(new JButton("Item 3"));

        frame.add(new JScrollPane(panel));
        frame.setTitle("Drag-and-Drop Panels with Buttons");
        frame.setSize(200, 160);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    });
}
```

## More Info

See https://www.marginallyclever.com/