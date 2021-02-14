package one.zookeeper.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class ZNodeName implements Comparable<ZNodeName> {

    private static final Logger log = LoggerFactory.getLogger(ZNodeName.class);

    private final String name;
    private final String prefix;
    private final Optional<Integer> sequence;

    public ZNodeName(final String name) {
        this.name = Objects.requireNonNull(name, "ZNode name cannot be null");

        final int idx = name.lastIndexOf("-");
        if (idx < 0) {
            this.prefix = name;
            this.sequence = Optional.empty();
        } else {
            this.prefix = name.substring(0, idx);
            this.sequence = Optional.ofNullable(parseSequenceString(name.substring(idx + 1)));
        }
    }

    private Integer parseSequenceString(final String seq) {
        try {
            return Integer.parseInt(seq);
        } catch (Exception e) {
            log.warn("Number format exception for {}", seq, e);
            return null;
        }
    }

    public String toString() {
        return "ZNodeName [name=" + name + ", prefix=" + prefix + ", sequence=" + sequence + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ZNodeName other = (ZNodeName) o;

        return name.equals(other.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(final ZNodeName that) {
        if (this.sequence.isPresent() && that.sequence.isPresent()) {
            int cseq = Integer.compare(this.sequence.get(), that.sequence.get());
            return (cseq != 0) ? cseq : this.prefix.compareTo(that.prefix);
        }
        if (this.sequence.isPresent()) {
            return -1;
        }
        if (that.sequence.isPresent()) {
            return 1;
        }
        return this.prefix.compareTo(that.prefix);
    }

    public String getName() {
        return name;
    }

    public Optional<Integer> getSequence() {
        return sequence;
    }

    public String getPrefix() {
        return prefix;
    }
}
