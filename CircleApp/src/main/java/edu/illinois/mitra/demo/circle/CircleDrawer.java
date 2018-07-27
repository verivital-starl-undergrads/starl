package edu.illinois.mitra.demo.circle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class CircleDrawer extends Drawer {

    private Stroke stroke = new BasicStroke(8);
    private Color selectColor = new Color(0,0,255,100);

    @Override
    public void draw(LogicThread lt, Graphics2D g) {
        Circle app = (Circle) lt;

        g.setColor(Color.RED);
        for (ItemPosition pos : app.destinations) {
            g.fillRect(pos.getX() - 13, pos.getY() - 13, 26, 26);
        }

        g.setColor(selectColor);
        g.setStroke(stroke);

    }

}
