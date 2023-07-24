/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.javadoc.internal.doclets.formats.html;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.internal.doclets.formats.html.markup.Comment;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.BaseOptions;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;


/**
 * Writes annotation interface member documentation in HTML format.
 */
public class AnnotationTypeMemberWriter extends AbstractMemberWriter {

    /**
     * We generate separate summaries for required and optional annotation interface members,
     * so we need dedicated writer instances for each kind. For the details section, a single
     * shared list is generated so a special {@code ANY} value is provided for this case.
     */
    enum Kind {
        OPTIONAL,
        REQUIRED,
        ANY
    }

    private final Kind kind;

    /**
     * The index of the current member that is being documented at this point
     * in time.
     */
    protected Element currentMember;

    /**
     * Constructs a new AnnotationTypeMemberWriterImpl for any kind of member.
     *
     * @param writer The writer for the class that the member belongs to.
     */
    public AnnotationTypeMemberWriter(SubWriterHolderWriter writer) {
        super(writer);
        this.kind = Kind.ANY;
    }

    /**
     * Constructs a new AnnotationTypeMemberWriterImpl for a specific kind of member.
     *
     * @param writer         the writer that will write the output.
     * @param annotationType the AnnotationType that holds this member.
     * @param kind           the kind of annotation interface members to handle.
     */
    public AnnotationTypeMemberWriter(SubWriterHolderWriter writer,
                                      TypeElement annotationType,
                                      Kind kind) {
        super(writer, annotationType);
        this.kind = kind;
    }

    public void build(Content target) throws DocletException {
        buildAnnotationTypeMember(target);
    }

    /**
     * Build the member documentation.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildAnnotationTypeMember(Content target) {
        // In contrast to the annotation interface member summaries the details generated
        // by this builder share a single list for both required and optional members.
        var members = getVisibleMembers(VisibleMemberTable.Kind.ANNOTATION_TYPE_MEMBER);
        if (!members.isEmpty()) {
            addAnnotationDetailsMarker(target);
            Content annotationDetailsHeader = getAnnotationDetailsHeader();
            Content memberList = getMemberList();

            for (Element member : members) {
                currentMember = member;
                Content annotationContent = getAnnotationHeaderContent(currentMember);

                buildAnnotationTypeMemberChildren(annotationContent);

                memberList.add(writer.getMemberListItem(annotationContent));
            }
            Content annotationDetails = getAnnotationDetails(annotationDetailsHeader, memberList);
            target.add(annotationDetails);
        }
    }

    protected void buildAnnotationTypeMemberChildren(Content annotationContent) {
        buildSignature(annotationContent);
        buildDeprecationInfo(annotationContent);
        buildPreviewInfo(annotationContent);
        buildMemberComments(annotationContent);
        buildTagInfo(annotationContent);
        buildDefaultValueInfo(annotationContent);
    }

    /**
     * Build the signature.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildSignature(Content target) {
        target.add(getSignature(currentMember));
    }

    /**
     * Build the deprecation information.
     *
     * @param annotationContent the content to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content annotationContent) {
        addDeprecated(currentMember, annotationContent);
    }

    /**
     * Build the preview information.
     *
     * @param annotationContent the content to which the documentation will be added
     */
    protected void buildPreviewInfo(Content annotationContent) {
        addPreview(currentMember, annotationContent);
    }

    /**
     * Build the comments for the member.  Do nothing if
     * {@link BaseOptions#noComment()} is set to true.
     *
     * @param annotationContent the content to which the documentation will be added
     */
    protected void buildMemberComments(Content annotationContent) {
        if (!options.noComment()) {
            addComments(currentMember, annotationContent);
        }
    }

    /**
     * Build the tag information.
     *
     * @param annotationContent the content to which the documentation will be added
     */
    protected void buildTagInfo(Content annotationContent) {
        addTags(currentMember, annotationContent);
    }

    /**
     * Build the default value for this optional member.
     *
     * @param annotationContent the content to which the documentation will be added
     */
    protected void buildDefaultValueInfo(Content annotationContent) {
        addDefaultValueInfo(currentMember, annotationContent);
    }

    @Override
    public Content getMemberSummaryHeader(TypeElement typeElement,
            Content content) {
        switch (kind) {
            case OPTIONAL -> content.add(selectComment(
                    MarkerComments.START_OF_ANNOTATION_TYPE_OPTIONAL_MEMBER_SUMMARY,
                    MarkerComments.START_OF_ANNOTATION_INTERFACE_OPTIONAL_MEMBER_SUMMARY));
            case REQUIRED -> content.add(selectComment(
                    MarkerComments.START_OF_ANNOTATION_TYPE_REQUIRED_MEMBER_SUMMARY,
                    MarkerComments.START_OF_ANNOTATION_INTERFACE_REQUIRED_MEMBER_SUMMARY));
            case ANY -> throw new UnsupportedOperationException("unsupported member kind");
        }
        Content c = new ContentBuilder();
        writer.addSummaryHeader(this, c);
        return c;
    }

    protected Content getMemberHeader() {
        return writer.getMemberHeader();
    }

    @Override
    public void addSummary(Content summariesList, Content content) {
        writer.addSummary(HtmlStyle.memberSummary,
                switch (kind) {
                    case REQUIRED -> HtmlIds.ANNOTATION_TYPE_REQUIRED_ELEMENT_SUMMARY;
                    case OPTIONAL -> HtmlIds.ANNOTATION_TYPE_OPTIONAL_ELEMENT_SUMMARY;
                    case ANY -> throw new UnsupportedOperationException("unsupported member kind");
                },
                summariesList, content);
    }

    protected void addAnnotationDetailsMarker(Content memberDetails) {
        memberDetails.add(selectComment(
                MarkerComments.START_OF_ANNOTATION_TYPE_DETAILS,
                MarkerComments.START_OF_ANNOTATION_INTERFACE_DETAILS));
    }

    protected Content getAnnotationDetailsHeader() {
        Content memberDetails = new ContentBuilder();
        var heading = HtmlTree.HEADING(Headings.TypeDeclaration.DETAILS_HEADING,
                contents.annotationTypeDetailsLabel);
        memberDetails.add(heading);
        return memberDetails;
    }

    protected Content getAnnotationHeaderContent(Element member) {
        Content content = new ContentBuilder();
        var heading = HtmlTree.HEADING(Headings.TypeDeclaration.MEMBER_HEADING,
                Text.of(name(member)));
        content.add(heading);
        return HtmlTree.SECTION(HtmlStyle.detail, content)
                .setId(htmlIds.forMember(typeElement, (ExecutableElement) member));
    }

    protected Content getSignature(Element member) {
        return new Signatures.MemberSignature(member, this)
                .setType(getType(member))
                .setAnnotations(writer.getAnnotationInfo(member, true))
                .toContent();
    }

    protected void addDeprecated(Element member, Content target) {
        addDeprecatedInfo(member, target);
    }

    protected void addPreview(Element member, Content content) {
        addPreviewInfo(member, content);
    }

    protected void addComments(Element member, Content annotationContent) {
        addComment(member, annotationContent);
    }

    protected void addTags(Element member, Content annotationContent) {
        writer.addTagsInfo(member, annotationContent);
    }

    protected Content getAnnotationDetails(Content annotationDetailsHeader, Content annotationDetails) {
        Content c = new ContentBuilder(annotationDetailsHeader, annotationDetails);
        return getMember(HtmlTree.SECTION(HtmlStyle.memberDetails, c));
    }

    @Override
    public void addSummaryLabel(Content content) {
        var label = HtmlTree.HEADING(Headings.TypeDeclaration.SUMMARY_HEADING,
                switch (kind) {
                    case REQUIRED -> contents.annotateTypeRequiredMemberSummaryLabel;
                    case OPTIONAL -> contents.annotateTypeOptionalMemberSummaryLabel;
                    case ANY -> throw new UnsupportedOperationException("unsupported member kind");
                });
        content.add(label);
    }

    /**
     * Get the caption for the summary table.
     * @return the caption
     */
    protected Content getCaption() {
        return contents.getContent(
                switch (kind) {
                    case REQUIRED -> "doclet.Annotation_Type_Required_Members";
                    case OPTIONAL -> "doclet.Annotation_Type_Optional_Members";
                    case ANY -> throw new UnsupportedOperationException("unsupported member kind");
                });
    }

    @Override
    public TableHeader getSummaryTableHeader(Element member) {
        return new TableHeader(contents.modifierAndTypeLabel,
                switch (kind) {
                    case REQUIRED -> contents.annotationTypeRequiredMemberLabel;
                    case OPTIONAL -> contents.annotationTypeOptionalMemberLabel;
                    case ANY -> throw new UnsupportedOperationException("unsupported member kind");
                },
                contents.descriptionLabel);
    }

    @Override
    protected Table<Element> createSummaryTable() {
        return new Table<Element>(HtmlStyle.summaryTable)
                .setCaption(getCaption())
                .setHeader(getSummaryTableHeader(typeElement))
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colSecond, HtmlStyle.colLast);
    }

    @Override
    public void addInheritedSummaryLabel(TypeElement typeElement, Content content) {
    }

    @Override
    protected void addSummaryLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element member,
                                  Content content) {
        Content memberLink = writer.getDocLink(context, utils.getEnclosingTypeElement(member), member,
                name(member), HtmlStyle.memberNameLink);
        var code = HtmlTree.CODE(memberLink);
        content.add(code);
    }

    @Override
    protected void addInheritedSummaryLink(TypeElement typeElement,
            Element member, Content target) {
        //Not applicable.
    }

    @Override
    protected void addSummaryType(Element member, Content content) {
        addModifiersAndType(member, getType(member), content);
    }

    @Override
    protected Content getSummaryLink(Element member) {
        String name = utils.getFullyQualifiedName(member) + "." + member.getSimpleName();
        return writer.getDocLink(HtmlLinkInfo.Kind.SHOW_PREVIEW, member, name);
    }

    protected Comment selectComment(Comment c1, Comment c2) {
        HtmlConfiguration configuration = writer.configuration;
        SourceVersion sv = configuration.docEnv.getSourceVersion();
        return sv.compareTo(SourceVersion.RELEASE_16) < 0 ? c1 : c2;
    }

    private TypeMirror getType(Element member) {
        return utils.isExecutableElement(member)
                ? utils.getReturnType(typeElement, (ExecutableElement) member)
                : member.asType();
    }

    public void addDefaultValueInfo(Element member, Content annotationContent) {
        if (utils.isAnnotationInterface(member.getEnclosingElement())) {
            ExecutableElement ee = (ExecutableElement) member;
            AnnotationValue value = ee.getDefaultValue();
            if (value != null) {
                var dl = HtmlTree.DL(HtmlStyle.notes);
                dl.add(HtmlTree.DT(contents.default_));
                dl.add(HtmlTree.DD(HtmlTree.CODE(Text.of(value.toString()))));
                annotationContent.add(dl);
            }
        }
    }
}