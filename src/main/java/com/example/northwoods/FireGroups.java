package com.example.northwoods;

import java.util.*;

/** Connected fire clusters, capped by distance rather than scan order. */
public final class FireGroups {
    public record Point(int x,int y,int z) {
        public double distanceSquared(Point p) { return (double)(x-p.x)*(x-p.x)+(double)(y-p.y)*(y-p.y)+(double)(z-p.z)*(z-p.z); }
    }
    public static List<List<Point>> nearest(List<Point> points, Point listener) {
        Set<Point> remaining=new HashSet<>(points);
        List<List<Point>> groups=new ArrayList<>();
        Comparator<Point> order=Comparator.comparingInt(Point::x).thenComparingInt(Point::y).thenComparingInt(Point::z);
        while(!remaining.isEmpty()) {
            List<Point> group=new ArrayList<>(); var queue=new ArrayDeque<Point>();
            Point first=remaining.iterator().next(); remaining.remove(first); queue.add(first);
            while(!queue.isEmpty()) {
                Point point=queue.remove(); group.add(point);
                List<Point> adjacent=remaining.stream().filter(p -> p.distanceSquared(point)<=9).toList();
                remaining.removeAll(adjacent); queue.addAll(adjacent);
            }
            group.sort(order); groups.add(List.copyOf(group));
        }
        groups.sort(Comparator.comparingDouble(g -> g.stream().mapToDouble(p->p.distanceSquared(listener)).min().orElse(Double.MAX_VALUE)));
        return groups.stream().limit(2).toList();
    }
}
