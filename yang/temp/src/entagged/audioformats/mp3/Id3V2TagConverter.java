/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Christian Laireiter <liree@web.de>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package entagged.audioformats.mp3;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import entagged.audioformats.generic.TagField;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;
import entagged.audioformats.mp3.util.id3frames.TimeId3Frame;

/**
 * This class converts the fields (frames) from
 * {@linkplain entagged.audioformats.mp3.Id3v2Tag tags} from an older to a newer
 * version.<br>
 * <p>
 * The best way to describe its function is to show an example:<br>
 * Let's convert an Id3v2.3 tag into the version 2.4 and illustrate the need of
 * a special handler (like this class).<br>
 * <p>
 * A major change in the version switch 2.3-&gt;2.4 occured on the information
 * of the recording time.<br>
 * To represent a fully qualified date and time for that in the Id3v2.4 you will
 * just need the field (frame) &quot;TDRC&quot; which is a textfield containing
 * formatted date-time description. It allows mulitple time-patterns to be used,
 * the most accurate variation is &quot;yyyy-MM-ddTHH:mm:ss&quot; with
 * &quot;THH&quot; representing the hour out of 24.
 * </p>
 * <p>
 * For the same information you need three different fields (frames) with
 * Id3v2.3. They are
 * <ul>
 * <li><b>TIME</b>: &quot;HHMM&quot; storing the time whithin the recorded day</li>
 * <li><b>TDAT</b>: &quot;DDMM&quot; storing the date whithin the year</li>
 * <li><b>TYER</b>: &quot;yyyy&quot; storing the year</li>
 * </ul>
 * </p>
 * <p>
 * So you see, If we want to convert this recoding time, we can't just simply
 * convert the names of the field. We must collect information and join the
 * data.<br>
 * Who knows what else must be handled.
 * </p>
 * </p>
 * 
 * @author Christian Laireiter
 */
public final class Id3V2TagConverter {

	/**
	 * This field maps the field names of the version 2 frames to the one of
	 * version 3.<br>
	 */
	private final static HashMap conversion22to23;

	/**
	 * Field name of the &quot;TDAT&quot; field of version v2.3.
	 */
	public final static String DATE = "TDAT";

	/**
	 * This field stores a set of field-names which will be discarded upon
	 * converstion to 2.4.<br>
	 * An example would be &quot;TRDA&quot;. It can take any data according to
	 * spec 2.3. Nothing you really could parse to a date information.
	 */
	private final static HashSet discard24;

	/**
	 * Field name of the &quot;TDAT&quot; field of version v2.3.
	 */
	public final static String RECORD_DAT = "TRDA";

	/**
	 * This field containts the frame names of those frames, which will be
	 * handled special by the v2.3 to v2.4 conversion.<be>
	 */
	private final static HashSet specialStore24;

	/**
	 * Field name of the &quot;TIME&quot; field of version v2.3.
	 */
	public final static String TIME = "TIME";

	/**
	 * Field name of the &quot;TYER&quot; field of version v2.3.
	 */
	public final static String YEAR = "TYER";

	/**
	 * Field name of the &quot;TDRC&quot; field of version 2.4.
	 */
	public final static String RECORDING_TIME = "TDRC";

	/*
	 * This static block initializes conversion22to23.
	 */
	static {
		conversion22to23 = new HashMap();
		String[] v22 = { "BUF", "CNT", "COM", "CRA", "CRM", "ETC", "EQU",
				"GEO", "IPL", "LNK", "MCI", "MLL", "PIC", "POP", "REV", "RVA",
				"SLT", "STC", "TAL", "TBP", "TCM", "TCO", "TCR", "TDA", "TDY",
				"TEN", "TFT", "TIM", "TKE", "TLA", "TLE", "TMT", "TOA", "TOF",
				"TOL", "TOR", "TOT", "TP1", "TP2", "TP3", "TP4", "TPA", "TPB",
				"TRC", "TRD", "TRK", "TSI", "TSS", "TT1", "TT2", "TT3", "TXT",
				"TXX", "TYE", "UFI", "ULT", "WAF", "WAR", "WAS", "WCM", "WCP",
				"WPB", "WXX" };
		String[] v23 = { "RBUF", "PCNT", "COMM", "AENC", "", "ETCO", "EQUA",
				"GEOB", "IPLS", "LINK", "MCDI", "MLLT", "APIC", "POPM", "RVRB",
				"RVAD", "SYLT", "SYTC", "TALB", "TBPM", "TCOM", "TCON", "TCOP",
				DATE, "TDLY", "TENC", "TFLT", TIME, "TKEY", "TLAN", "TLEN",
				"TMED", "TOPE", "TOFN", "TOLY", "TORY", "TOAL", "TPE1", "TPE2",
				"TPE3", "TPE4", "TPOS", "TPUB", "TSRC", RECORD_DAT, "TRCK",
				"TSIZ", "TSSE", "TIT1", "TIT2", "TIT3", "TEXT", "TXXX", YEAR,
				"UFID", "USLT", "WOAF", "WOAR", "WOAS", "WCOM", "WCOP", "WPUB",
				"WXXX" };
		for (int i = 0; i < v22.length; i++) {
			conversion22to23.put(v22[i], v23[i]);
		}
		specialStore24 = new HashSet(Arrays.asList(new String[] { TIME, YEAR,
				DATE }));
		discard24 = new HashSet(Arrays.asList(new String[] { RECORD_DAT }));
		discard24.addAll(specialStore24);
	}

	/**
	 * This method will convert the given <code>tag</code> into the given
	 * Id3v2 version.<br>
	 * Allowed values for <code>targetVersion</code> are:<br>
	 * <ul>
	 * <li> {@link Id3v2Tag#ID3V23} </li>
	 * <li> {@link Id3v2Tag#ID3V24} </li>
	 * </ul>
	 * The {@linkplain Id3v2Tag#getRepresentedVersion() version} of the given
	 * <code>tag</code> must be lower than the one to be converted two.<br>
	 * <br>
	 * 
	 * @param tag
	 *            The tag to be converted.
	 * @param targetVersion
	 *            The version to be converted to.
	 * @return The converted tag.
	 */
	public static Id3v2Tag convert(Id3v2Tag tag, int targetVersion) {
		assert tag != null
				&& (targetVersion == Id3v2Tag.ID3V22
						|| targetVersion == Id3v2Tag.ID3V23 || targetVersion == Id3v2Tag.ID3V24)
				&& (tag.getRepresentedVersion() == Id3v2Tag.ID3V22
						|| tag.getRepresentedVersion() == Id3v2Tag.ID3V23 || tag
						.getRepresentedVersion() == Id3v2Tag.ID3V24);
		Id3v2Tag result = null;
		if (targetVersion <= tag.getRepresentedVersion()) {
			// return the given tag, since only upward conversion is implemented
			result = tag;
		} else {
			if (tag.getRepresentedVersion() < Id3v2Tag.ID3V23) {
				result = convert22to23(tag);
			}
			if (tag.getRepresentedVersion() < Id3v2Tag.ID3V24
					&& targetVersion <= Id3v2Tag.ID3V24) {
				// convert from Id3v2.3 to Id3v2.4
				result = convert23to24(result);
			}
		}
		assert result != null;
		return result;
	}

	/**
	 * This method converts the given tag from Id3v2.2 to Id3v2.3.<br>
	 * 
	 * @param source
	 *            The tag to be converted.
	 * @return A new object containing the converted data.<br>
	 */
	private static Id3v2Tag convert22to23(Id3v2Tag source) {
		assert source != null
				&& source.getRepresentedVersion() == Id3v2Tag.ID3V22;
		Iterator fields = source.getFields();
		while (fields.hasNext()) {
			TagField current = (TagField) fields.next();
			String currentId = current.getId();
			String conv = (String) conversion22to23.get(currentId);
			if (currentId.equals(conv)) {
				fields.remove();
				if (current instanceof TextId3Frame) {
					source.add(new TextId3Frame(conv, ((TextId3Frame) current)
							.getContent()));
				}
			}
		}
		source.setRepresentedVersion(Id3v2Tag.ID3V23);
		return source;
	}

	/**
	 * This method converts the given tag from Id3v2.3 to Id3v2.4.<br>
	 * 
	 * @param source
	 *            The tag to be converted.
	 * @return A new object containing the converted data.<br>
	 */
	private static Id3v2Tag convert23to24(Id3v2Tag source) {
		assert source != null
				&& source.getRepresentedVersion() == Id3v2Tag.ID3V22;
		Iterator fields = source.getFields();
		HashMap specialStore = new HashMap();
		while (fields.hasNext()) {
			TagField current = (TagField) fields.next();
			if (specialStore24.contains(current.getId())) {
				specialStore.put(current.getId(), current);
			}
			if (discard24.contains(current.getId())) {
				fields.remove();
			}
		}
		/*
		 * Now convert some Special Fields.
		 */
		// TDAT, TIME and TYEAR -> to -> TDRC
		TimeId3Frame tdrc = createTimeField((TextId3Frame) specialStore
				.get(DATE), (TextId3Frame) specialStore.get(TIME),
				(TextId3Frame) specialStore.get(YEAR));
		source.set(tdrc);
		source.setRepresentedVersion(Id3v2Tag.ID3V24);
		return source;
	}

	/**
	 * This method creates a {@link TimeId3Frame} from given Textfields.<br>
	 * This is a convenience method for the conversion of Id3 version 2.3 tags
	 * into 2.4.<br>
	 * If all of the parameters are <code>null</code>, a timestamp with zero
	 * data will be returned.
	 * 
	 * @param tdat
	 *            The old TDAT field. Maybe <code>null</code>.
	 * @param time
	 *            The old TIME field. Maybe <code>null</code>
	 * @param tyer
	 *            The old TYER field. Maybe <code>null</code>
	 * @return A time field containing given data.
	 */
	private static TimeId3Frame createTimeField(TextId3Frame tdat,
			TextId3Frame time, TextId3Frame tyer) {
		TimeId3Frame result = null;
		Calendar calendar = new GregorianCalendar();
		calendar.clear();
		try {
			if (tdat != null) {
				if (tdat.getContent().length() == 4) {
					calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tdat
							.getContent().substring(0, 2)));
					calendar.set(Calendar.MONTH, Integer.parseInt(tdat
							.getContent().substring(2, 4)) - 1);
				} else {
					System.err
							.println("Field TDAT ignroed, since it is not spec conform: \""
									+ tdat.getContent() + "\"");
				}
			}
			if (time != null) {
				if (time.getContent().length() == 4) {
					calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time
							.getContent().substring(0, 2)));
					calendar.set(Calendar.MINUTE, Integer.parseInt(time
							.getContent().substring(2, 4)));
				} else {
					System.err
							.println("Field TIME ignroed, since it is not spec conform: \""
									+ time.getContent() + "\"");
				}
			}
			if (tyer != null) {
				if (tyer.getContent().length() == 4) {
					calendar.set(Calendar.YEAR, Integer.parseInt(tyer
							.getContent()));
				} else {
					System.err
							.println("Field TYER ignroed, since it is not spec conform: \""
									+ tyer.getContent() + "\"");
				}
			}
			result = new TimeId3Frame(RECORD_DAT, calendar);
		} catch (NumberFormatException e) {
			System.err.println("Numberformatexception occured "
					+ "in timestamp interpretation, date is set to zero.");
			e.printStackTrace();
			calendar.clear();
		}
		return result;
	}

}
