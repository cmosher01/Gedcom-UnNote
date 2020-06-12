package nu.mine.mosher.gedcom;

@SuppressWarnings({"access", "WeakerAccess", "unused"})
public class GedcomUnNoteOptions extends GedcomOptions {
    public enum Target {INLINE, RECORD}

    public Target target;
    public boolean delete;

    public void help() {
        this.help = true;
        System.err.println("Usage: gedcom-unnote [OPTIONS] <in.ged >out.ged");
        System.err.println("Removes empty NOTE records, or converts between inline NOTEs and NOTE records.");
        System.err.println("Options:");
        System.err.println("-d, --delete               Deletes empty NOTEs.");
        System.err.println("-n, --note{inline|record}  Converts NOTEs to record/inline.");
        System.err.println("                           Never inlines NOTEs with multiple pointers.");
        options();
    }

    public void d() {
        delete();
    }

    public void delete() {
        this.delete = true;
    }

    public void n(final String to) {
        note(to);
    }

    public void note(final String to) {
        this.target = Target.valueOf(to.toUpperCase());
    }

    public GedcomUnNoteOptions verify() {
        if (this.help) {
            return this;
        }
        if (this.target == null && !this.delete) {
            throw new IllegalArgumentException("Missing: -d or -n.");
        }
        if (this.target != null && this.delete) {
            throw new IllegalArgumentException("Can only use one of: -d or -n.");
        }
        if (this.concToWidth == null) {
            throw new IllegalArgumentException("The -c option is required.");
        }
        return this;
    }
}
