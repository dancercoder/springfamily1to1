package one.zookeeper.lock;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ZnodeNameTest {

    @Test
    public void testOrderWithSamePrefix() throws Exception {
        final String[] names = {"x-3", "x-5", "x-11", "x-1"};
        ZNodeName zname;

        final Collection<ZNodeName> nodeNames = Arrays.asList(names).stream()
                .map(name -> new ZNodeName(name)).sorted().collect(Collectors.toList());

        final Iterator<ZNodeName> it = nodeNames.iterator();

        zname = it.next();
        assertEquals("x-1", zname.getName());
        assertEquals("x", zname.getPrefix());
        assertEquals(Integer.valueOf(1), zname.getSequence().get());

        zname = it.next();
        assertEquals("x-3", zname.getName());
        assertEquals("x", zname.getPrefix());
        assertEquals(Integer.valueOf(3), zname.getSequence().get());

        zname = it.next();
        assertEquals("x-5", zname.getName());
        assertEquals("x", zname.getPrefix());
        assertEquals(Integer.valueOf(5), zname.getSequence().get());

        zname = it.next();
        assertEquals("x-11", zname.getName());
        assertEquals("x", zname.getPrefix());
        assertEquals(Integer.valueOf(11), zname.getSequence().get());
    }

    @Test
    public void testOrderWithDifferentPrefixes() throws Exception {
        final String[] names = {"r-3", "r-2", "r-1", "w-2", "w-1"};
        ZNodeName zname;

        final Collection<ZNodeName> nodeNames = Arrays.asList(names).stream()
                .map(name -> new ZNodeName(name)).sorted().collect(Collectors.toList());

        final Iterator<ZNodeName> it = nodeNames.iterator();

        zname = it.next();
        assertEquals("r-1", zname.getName());
        assertEquals("r", zname.getPrefix());
        assertEquals(Integer.valueOf(1), zname.getSequence().get());

        zname = it.next();
        assertEquals("w-1", zname.getName());
        assertEquals("w", zname.getPrefix());
        assertEquals(Integer.valueOf(1), zname.getSequence().get());

        zname = it.next();
        assertEquals("r-2", zname.getName());
        assertEquals("r", zname.getPrefix());
        assertEquals(Integer.valueOf(2), zname.getSequence().get());

        zname = it.next();
        assertEquals("w-2", zname.getName());
        assertEquals("w", zname.getPrefix());
        assertEquals(Integer.valueOf(2), zname.getSequence().get());

        zname = it.next();
        assertEquals("r-3", zname.getName());
        assertEquals("r", zname.getPrefix());
        assertEquals(Integer.valueOf(3), zname.getSequence().get());
    }

    @Test
    public void testOrderWithDifferentPrefixIncludingSessionId() throws Exception {
        String[] names = {
                "x-242681582799028564-0000000002",
                "x-170623981976748329-0000000003",
                "x-98566387950223723-0000000001"
        };
        ZNodeName zname;

        final Collection<ZNodeName> nodeNames = Arrays.asList(names).stream()
                .map(name -> new ZNodeName(name)).sorted().collect(Collectors.toList());

        final Iterator<ZNodeName> it = nodeNames.iterator();

        zname = it.next();
        assertEquals("x-98566387950223723-0000000001", zname.getName());
        assertEquals("x-98566387950223723", zname.getPrefix());
        assertEquals(Integer.valueOf(1), zname.getSequence().get());

        zname = it.next();
        assertEquals("x-242681582799028564-0000000002", zname.getName());
        assertEquals("x-242681582799028564", zname.getPrefix());
        assertEquals(Integer.valueOf(2), zname.getSequence().get());

        zname = it.next();
        assertEquals("x-170623981976748329-0000000003", zname.getName());
        assertEquals("x-170623981976748329", zname.getPrefix());
        assertEquals(Integer.valueOf(3), zname.getSequence().get());
    }


    @Test
    public void testOrderWithExtraPrefixes() throws Exception {
        String[] names = {"r-1-3-2", "r-2-2-1", "r-3-1-3"};
        ZNodeName zname;

        final Collection<ZNodeName> nodeNames = Arrays.asList(names).stream()
                .map(name -> new ZNodeName(name)).sorted().collect(Collectors.toList());

        final Iterator<ZNodeName> it = nodeNames.iterator();

        zname = it.next();
        assertEquals("r-2-2-1", zname.getName());
        assertEquals("r-2-2", zname.getPrefix());
        assertEquals(Integer.valueOf(1), zname.getSequence().get());

        zname = it.next();
        assertEquals("r-1-3-2", zname.getName());
        assertEquals("r-1-3", zname.getPrefix());
        assertEquals(Integer.valueOf(2), zname.getSequence().get());

        zname = it.next();
        assertEquals("r-3-1-3", zname.getName());
        assertEquals("r-3-1", zname.getPrefix());
        assertEquals(Integer.valueOf(3), zname.getSequence().get());
    }

    @Test
    public void testMissingSequenceNumber() throws Exception {
        String[] names = {"c", "b-1", "a"};
        ZNodeName zname;

        final Collection<ZNodeName> nodeNames = Arrays.asList(names).stream()
                .map(name -> new ZNodeName(name)).sorted().collect(Collectors.toList());

        final Iterator<ZNodeName> it = nodeNames.iterator();

        zname = it.next();
        assertEquals("b-1", zname.getName());
        assertEquals("b", zname.getPrefix());
        assertEquals(Integer.valueOf(1), zname.getSequence().get());

        zname = it.next();
        assertEquals("a", zname.getName());
        assertEquals("a", zname.getPrefix());
        assertFalse(zname.getSequence().isPresent());

        zname = it.next();
        assertEquals("c", zname.getName());
        assertEquals("c", zname.getPrefix());
        assertFalse(zname.getSequence().isPresent());
    }


    @Test
    public void testNullName() {
        assertThrows(NullPointerException.class, () -> {
            new ZNodeName(null);
        });
    }
}