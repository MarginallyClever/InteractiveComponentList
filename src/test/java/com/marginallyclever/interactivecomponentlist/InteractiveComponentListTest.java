package com.marginallyclever.interactivecomponentlist;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import javax.swing.*;
import java.awt.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "headless environment")
class InteractiveComponentListTest {
    private FrameFixture window;
    private InteractiveComponentList list;
    private final JButton b1 = new JButton("Element 1");
    private final JButton b2 = new JButton("Element 2");
    private final JButton b3 = new JButton("Element 3");

    @BeforeEach
    void setUp() {
        Robot robot = BasicRobot.robotWithNewAwtHierarchy();
        list = new InteractiveComponentList();
        list.setName("myList");
        list.add(b1);
        list.add(b2);
        list.add(b3);

        b1.setName("Element 1");
        b2.setName("Element 2");
        b3.setName("Element 3");

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame();
            f.add(new JScrollPane(list));
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null);
            //f.pack();
            return f;
        });
        frame.setSize(400, 300);
        window = new FrameFixture(robot, frame);
        window.show(); // shows the frame to test
    }

    @AfterEach
    protected void tearDown() {
        if(window!=null) window.cleanUp();
    }

    @Test
    public void testListOperations() {
        AtomicBoolean click1 = new AtomicBoolean(false);
        AtomicBoolean click2 = new AtomicBoolean(false);
        AtomicBoolean click3 = new AtomicBoolean(false);

        b1.addActionListener(e -> click1.set(true));
        b2.addActionListener(e -> click2.set(true));
        b3.addActionListener(e -> click3.set(true));

        assertEquals(3, list.getComponentCount(), "List should have 3 elements after adding.");

        // Click on elements
        window.button("Element 1").click();
        assertTrue(click1.get(), "Element 1 should have been clicked.");
        window.button("Element 2").click();
        assertTrue(click2.get(), "Element 2 should have been clicked.");
        window.button("Element 3").click();
        assertTrue(click3.get(), "Element 3 should have been clicked.");
    }

    // Drag first item to bottom of list
    @Test
    public void testDragToBottom() {
        // Step 1: Obtain the drag handle position of the first component
        Component c = list.getComponent(0).getComponentAt(5, 5);
        Point dragHandlePosition = getDragHandlePosition(c);
        // Step 2: Calculate the target position
        Point targetPosition = new Point(dragHandlePosition.x, dragHandlePosition.y + 110);
        // Convert targetPosition to screen coordinates
        Point listLocationOnScreen = list.getLocationOnScreen();
        Point targetScreenPosition = new Point(listLocationOnScreen.x + targetPosition.x, listLocationOnScreen.y + targetPosition.y);

        doMove(c,dragHandlePosition,targetScreenPosition);
        // confirm all good
        assertEquals(b1, list.getInnerComponent(2), "Element 1 should now be last.");
        assertEquals(b2, list.getInnerComponent(0), "Element 2 should now be first.");
    }

    // drag second item to top
    @Test
    public void testDragToTop() {
        // Step 1: Obtain the drag handle position of the first component
        Component c = list.getComponent(1).getComponentAt(5, 5);
        Point dragHandlePosition = getDragHandlePosition(c);
        // Step 2: Calculate the target position
        Point targetPosition = new Point(dragHandlePosition.x, dragHandlePosition.y );
        // Convert targetPosition to screen coordinates
        Point listLocationOnScreen = list.getLocationOnScreen();
        Point targetScreenPosition = new Point(listLocationOnScreen.x + targetPosition.x, listLocationOnScreen.y + targetPosition.y);

        doMove(c,dragHandlePosition,targetScreenPosition);
        // confirm all good
        assertEquals(b1, list.getInnerComponent(1), "Element 1 should now be second.");
        assertEquals(b2, list.getInnerComponent(0), "Element 2 should now be first.");
    }

    private void doMove(Component c,Point dragHandlePosition,Point targetScreenPosition) {
        var robot = window.panel("myList").robot();
        robot.pressMouse(c, dragHandlePosition);
        robot.moveMouse(targetScreenPosition);
        robot.releaseMouseButtons();
        // give the UI time to update
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignore) {}
    }

    private Point getDragHandlePosition(Component component) {
        Rectangle bounds = component.getBounds();
        int x = bounds.x + bounds.width / 2;
        int y = bounds.y; // Top center position
        return new Point(x, y);
    }

    private Point getTargetPositionForComponent(Component component) {
        Rectangle bounds = component.getBounds();
        int x = bounds.x + bounds.width / 2;
        int y = bounds.y + bounds.height; // Bottom center position
        return new Point(x, y);
    }
}