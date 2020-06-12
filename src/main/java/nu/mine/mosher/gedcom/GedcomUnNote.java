package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;
import nu.mine.mosher.mopper.ArgParser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static nu.mine.mosher.gedcom.GedcomUnNoteOptions.Target.*;
import static nu.mine.mosher.logging.Jul.log;

// Created by Christopher Alan Mosher on 2017-08-28

public class GedcomUnNote implements Gedcom.Processor {
    private final GedcomUnNoteOptions options;
    private final Map<String, AtomicInteger> mapIdToCount = new HashMap<>(256);
    private final List<TreeNode<GedcomLine>> deletes = new ArrayList<>(256);
    private final List<TreeNode<GedcomLine>> inserts = new ArrayList<>(256);
    private GedcomTree tree;

    public static void main(final String... args) throws InvalidLevel, IOException {
        log();
        final GedcomUnNoteOptions options = new ArgParser<>(new GedcomUnNoteOptions()).parse(args).verify();
        new Gedcom(options, new GedcomUnNote(options)).main();
        System.out.flush();
        System.err.flush();
    }

    private GedcomUnNote(final GedcomUnNoteOptions options) {
        this.options = options;
    }

    @Override
    public boolean process(final GedcomTree tree) {
        this.tree = tree;
        try {
            if (this.options.delete) {
                flagEmptyNotes(this.tree.getRoot());
            } else if (this.options.target.equals(INLINE)) {
                countAllNotePointers(this.tree.getRoot());
                inlineNotes(this.tree.getRoot());
            } else if (this.options.target.equals(RECORD)) {
                recordNotes(this.tree.getRoot());
            }
            insertNewTopLevelNotes();
            deleteFlaggedNodes();
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    private void recordNotes(final TreeNode<GedcomLine> node) {
        node.forEach(this::recordNotes);

        final GedcomLine line = node.getObject();
        if (line == null) {
            return;
        }

        if (!line.getTag().equals(GedcomTag.NOTE)) {
            return;
        }

        if (line.isPointer()) {
            return;
        }

        if (line.hasID()) {
            return;
        }

        final GedcomLine recordNote = GedcomLine.createUid(GedcomTag.NOTE, line.getValue());
        this.inserts.add(new TreeNode<>(recordNote));
        node.setObject(line.replacePointer(recordNote));
    }

    private void inlineNotes(final TreeNode<GedcomLine> node) {
        node.forEach(this::inlineNotes);

        final GedcomLine line = node.getObject();
        if (line == null) {
            return;
        }

        if (!line.getTag().equals(GedcomTag.NOTE)) {
            return;
        }

        if (!line.isPointer()) {
            return;
        }

        final String id = line.getPointer();
        if (this.mapIdToCount.get(id).get() > 1) {
            log().warning("Will not inline NOTE (because it has more than one reference): " + id);
            return;
        }

        if (!canDelete(node)) {
            return;
        }

        final TreeNode<GedcomLine> nodeRecord = this.tree.getNode(id);
        node.setObject(line.replaceValue(nodeRecord.getObject().getValue().trim()));
        flagForDelete(nodeRecord);
    }

    private void countAllNotePointers(final TreeNode<GedcomLine> node) {
        node.forEach(this::countAllNotePointers);

        final GedcomLine line = node.getObject();
        if (line == null) {
            return;
        }

        if (!line.getTag().equals(GedcomTag.NOTE)) {
            return;
        }

        if (!line.isPointer()) {
            return;
        }

        final String id = line.getPointer();
        if (!this.mapIdToCount.containsKey(id)) {
            this.mapIdToCount.put(id, new AtomicInteger());
        }

        this.mapIdToCount.get(id).incrementAndGet();
    }

    private void flagEmptyNotes(final TreeNode<GedcomLine> node) {
        node.forEach(this::flagEmptyNotes);

        final GedcomLine line = node.getObject();
        if (line == null) {
            return;
        }

        if (!line.getTag().equals(GedcomTag.NOTE)) {
            return;
        }

        if (!isNoteEmpty(node)) {
            return;
        }

        flagForDelete(node);
    }

    private boolean canDelete(final TreeNode<GedcomLine> nodeNote) {
        final GedcomLine lineNote = nodeNote.getObject();
        final TreeNode<GedcomLine> nodeRecord;
        if (lineNote.isPointer()) {
            nodeRecord = this.tree.getNode(lineNote.getPointer());
        } else {
            nodeRecord = null;
        }

        if (hasChild(nodeNote, GedcomTag.SOUR)) {
            log().warning("Will not remove NOTE (because it has a SOUR): " + nodeNote);
            return false;
        }

        if (nodeRecord != null && hasChild(nodeRecord, GedcomTag.SOUR)) {
            log().warning("Will not remove NOTE (because it has a SOUR): " + nodeRecord);
            return false;
        }

        return true;
    }

    private void flagForDelete(final TreeNode<GedcomLine> nodeNote) {
        if (!canDelete(nodeNote)) {
            return;
        }

        final GedcomLine lineNote = nodeNote.getObject();
        final TreeNode<GedcomLine> nodeRecord;
        if (lineNote.isPointer()) {
            nodeRecord = this.tree.getNode(lineNote.getPointer());
        } else {
            nodeRecord = null;
        }

        this.deletes.add(nodeNote);
        if (nodeRecord != null) {
            this.deletes.add(nodeRecord);
        }
    }

    private boolean isNoteEmpty(final TreeNode<GedcomLine> nodeNote) {
        final GedcomLine lineNote = nodeNote.getObject();
        final GedcomLine lineValue;
        if (lineNote.isPointer()) {
            lineValue = this.tree.getNode(lineNote.getPointer()).getObject();
        } else {
            lineValue = lineNote;
        }
        return lineValue.getValue().trim().isEmpty();
    }

    private void deleteFlaggedNodes() {
        // TODO log at info level?
        this.deletes.forEach(TreeNode::removeFromParent);
    }

    private void insertNewTopLevelNotes() {
        // TODO log at info level?
        final TreeNode<GedcomLine> firstNote = findFirstNote();
        this.inserts.forEach(n -> this.tree.getRoot().addChildBefore(n, firstNote));
    }

    private TreeNode<GedcomLine> findFirstNote() {
        for (final TreeNode<GedcomLine> top : this.tree.getRoot()) {
            final GedcomTag tag = top.getObject().getTag();
            if (tag.equals(GedcomTag.NOTE) || tag.equals(GedcomTag.TRLR)) {
                return top;
            }
        }
        // This will only happen if there are no NOTE records nor a TRLR record:
        return null;
    }

    private static boolean hasChild(final TreeNode<GedcomLine> node, final GedcomTag tag) {
        for (final TreeNode<GedcomLine> c : node) {
            if (c.getObject().getTag().equals(tag)) {
                return true;
            }
        }
        return false;
    }
}
