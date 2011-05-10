//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.09 at 02:35:55 PM BST 
//


package org.atlasapi.remotesite.pa.bindings;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "showingId",
    "progId",
    "seriesId",
    "rtFilmnumber",
    "date",
    "time",
    "duration",
    "group",
    "title",
    "episodeTitle",
    "episodeNumber",
    "episodeTotal",
    "seriesNumber",
    "category",
    "rtCategory",
    "filmYear",
    "genre",
    "certificate",
    "country",
    "colour",
    "warning",
    "subtitles",
    "dubbing",
    "starRating",
    "castMember",
    "staffMember",
    "billings",
    "videoplus",
    "attr",
    "roviGenre",
    "roviDescription",
    "pictures"
})
@XmlRootElement(name = "prog_data")
public class ProgData {

    @XmlElement(name = "showing_id", required = true)
    protected String showingId;
    @XmlElement(name = "prog_id", required = true)
    protected String progId;
    @XmlElement(name = "series_id")
    protected String seriesId;
    @XmlElement(name = "rt_filmnumber")
    protected String rtFilmnumber;
    @XmlElement(required = true)
    protected String date;
    @XmlElement(required = true)
    protected String time;
    @XmlElement(required = true)
    protected String duration;
    protected String group;
    @XmlElement(required = true)
    protected String title;
    @XmlElement(name = "episode_title")
    protected String episodeTitle;
    @XmlElement(name = "episode_number")
    protected String episodeNumber;
    @XmlElement(name = "episode_total")
    protected String episodeTotal;
    @XmlElement(name = "series_number")
    protected String seriesNumber;
    protected List<Category> category;
    @XmlElement(name = "rt_category")
    protected RtCategory rtCategory;
    @XmlElement(name = "film_year")
    protected String filmYear;
    protected String genre;
    protected String certificate;
    protected String country;
    protected String colour;
    protected Warning warning;
    protected String subtitles;
    protected String dubbing;
    @XmlElement(name = "star_rating")
    protected String starRating;
    @XmlElement(name = "cast_member")
    protected List<CastMember> castMember;
    @XmlElement(name = "staff_member")
    protected List<StaffMember> staffMember;
    protected Billings billings;
    @XmlElement(required = true)
    protected String videoplus;
    @XmlElement(required = true)
    protected Attr attr;
    @XmlElement(name = "rovi_genre")
    protected String roviGenre;
    @XmlElement(name = "rovi_description")
    protected String roviDescription;
    protected Pictures pictures;

    /**
     * Gets the value of the showingId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShowingId() {
        return showingId;
    }

    /**
     * Sets the value of the showingId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShowingId(String value) {
        this.showingId = value;
    }

    /**
     * Gets the value of the progId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProgId() {
        return progId;
    }

    /**
     * Sets the value of the progId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProgId(String value) {
        this.progId = value;
    }

    /**
     * Gets the value of the seriesId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSeriesId() {
        return seriesId;
    }

    /**
     * Sets the value of the seriesId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSeriesId(String value) {
        this.seriesId = value;
    }

    /**
     * Gets the value of the rtFilmnumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRtFilmnumber() {
        return rtFilmnumber;
    }

    /**
     * Sets the value of the rtFilmnumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRtFilmnumber(String value) {
        this.rtFilmnumber = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDate(String value) {
        this.date = value;
    }

    /**
     * Gets the value of the time property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the value of the time property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTime(String value) {
        this.time = value;
    }

    /**
     * Gets the value of the duration property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Sets the value of the duration property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDuration(String value) {
        this.duration = value;
    }

    /**
     * Gets the value of the group property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the value of the group property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroup(String value) {
        this.group = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the episodeTitle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEpisodeTitle() {
        return episodeTitle;
    }

    /**
     * Sets the value of the episodeTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEpisodeTitle(String value) {
        this.episodeTitle = value;
    }

    /**
     * Gets the value of the episodeNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEpisodeNumber() {
        return episodeNumber;
    }

    /**
     * Sets the value of the episodeNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEpisodeNumber(String value) {
        this.episodeNumber = value;
    }

    /**
     * Gets the value of the episodeTotal property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEpisodeTotal() {
        return episodeTotal;
    }

    /**
     * Sets the value of the episodeTotal property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEpisodeTotal(String value) {
        this.episodeTotal = value;
    }

    /**
     * Gets the value of the seriesNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSeriesNumber() {
        return seriesNumber;
    }

    /**
     * Sets the value of the seriesNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSeriesNumber(String value) {
        this.seriesNumber = value;
    }

    /**
     * Gets the value of the category property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the category property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCategory().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Category }
     * 
     * 
     */
    public List<Category> getCategory() {
        if (category == null) {
            category = new ArrayList<Category>();
        }
        return this.category;
    }

    /**
     * Gets the value of the rtCategory property.
     * 
     * @return
     *     possible object is
     *     {@link RtCategory }
     *     
     */
    public RtCategory getRtCategory() {
        return rtCategory;
    }

    /**
     * Sets the value of the rtCategory property.
     * 
     * @param value
     *     allowed object is
     *     {@link RtCategory }
     *     
     */
    public void setRtCategory(RtCategory value) {
        this.rtCategory = value;
    }

    /**
     * Gets the value of the filmYear property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFilmYear() {
        return filmYear;
    }

    /**
     * Sets the value of the filmYear property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFilmYear(String value) {
        this.filmYear = value;
    }

    /**
     * Gets the value of the genre property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGenre() {
        return genre;
    }

    /**
     * Sets the value of the genre property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGenre(String value) {
        this.genre = value;
    }

    /**
     * Gets the value of the certificate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * Sets the value of the certificate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCertificate(String value) {
        this.certificate = value;
    }

    /**
     * Gets the value of the country property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the value of the country property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountry(String value) {
        this.country = value;
    }

    /**
     * Gets the value of the colour property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getColour() {
        return colour;
    }

    /**
     * Sets the value of the colour property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setColour(String value) {
        this.colour = value;
    }

    /**
     * Gets the value of the warning property.
     * 
     * @return
     *     possible object is
     *     {@link Warning }
     *     
     */
    public Warning getWarning() {
        return warning;
    }

    /**
     * Sets the value of the warning property.
     * 
     * @param value
     *     allowed object is
     *     {@link Warning }
     *     
     */
    public void setWarning(Warning value) {
        this.warning = value;
    }

    /**
     * Gets the value of the subtitles property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubtitles() {
        return subtitles;
    }

    /**
     * Sets the value of the subtitles property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubtitles(String value) {
        this.subtitles = value;
    }

    /**
     * Gets the value of the dubbing property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDubbing() {
        return dubbing;
    }

    /**
     * Sets the value of the dubbing property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDubbing(String value) {
        this.dubbing = value;
    }

    /**
     * Gets the value of the starRating property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStarRating() {
        return starRating;
    }

    /**
     * Sets the value of the starRating property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStarRating(String value) {
        this.starRating = value;
    }

    /**
     * Gets the value of the castMember property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the castMember property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCastMember().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CastMember }
     * 
     * 
     */
    public List<CastMember> getCastMember() {
        if (castMember == null) {
            castMember = new ArrayList<CastMember>();
        }
        return this.castMember;
    }

    /**
     * Gets the value of the staffMember property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the staffMember property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStaffMember().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StaffMember }
     * 
     * 
     */
    public List<StaffMember> getStaffMember() {
        if (staffMember == null) {
            staffMember = new ArrayList<StaffMember>();
        }
        return this.staffMember;
    }

    /**
     * Gets the value of the billings property.
     * 
     * @return
     *     possible object is
     *     {@link Billings }
     *     
     */
    public Billings getBillings() {
        return billings;
    }

    /**
     * Sets the value of the billings property.
     * 
     * @param value
     *     allowed object is
     *     {@link Billings }
     *     
     */
    public void setBillings(Billings value) {
        this.billings = value;
    }

    /**
     * Gets the value of the videoplus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVideoplus() {
        return videoplus;
    }

    /**
     * Sets the value of the videoplus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVideoplus(String value) {
        this.videoplus = value;
    }

    /**
     * Gets the value of the attr property.
     * 
     * @return
     *     possible object is
     *     {@link Attr }
     *     
     */
    public Attr getAttr() {
        return attr;
    }

    /**
     * Sets the value of the attr property.
     * 
     * @param value
     *     allowed object is
     *     {@link Attr }
     *     
     */
    public void setAttr(Attr value) {
        this.attr = value;
    }

    /**
     * Gets the value of the roviGenre property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRoviGenre() {
        return roviGenre;
    }

    /**
     * Sets the value of the roviGenre property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRoviGenre(String value) {
        this.roviGenre = value;
    }

    /**
     * Gets the value of the roviDescription property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRoviDescription() {
        return roviDescription;
    }

    /**
     * Sets the value of the roviDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRoviDescription(String value) {
        this.roviDescription = value;
    }

    /**
     * Gets the value of the pictures property.
     * 
     * @return
     *     possible object is
     *     {@link Pictures }
     *     
     */
    public Pictures getPictures() {
        return pictures;
    }

    /**
     * Sets the value of the pictures property.
     * 
     * @param value
     *     allowed object is
     *     {@link Pictures }
     *     
     */
    public void setPictures(Pictures value) {
        this.pictures = value;
    }

}
