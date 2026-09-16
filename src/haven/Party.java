/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.awt.Color;
import java.util.regex.Pattern;

public class Party {
    public Map<Long, Member> memb = Collections.emptyMap();
    public Member leader = null;
    public int id;
    private final Glob glob;
    private int mseq = 0;

    public Party(Glob glob) {
	this.glob = glob;
    }

    public class Member {
	public final long gobid;
	public final int seq;
	private Coord2d c = null;
	private double ma = Math.random() * Math.PI * 2;
	private double oa = Double.NaN;
	public Color col = Color.BLACK;

	public Member(long gobid) {
	    this.gobid = gobid;
	    this.seq = mseq++;
	}

	public Gob getgob() {
	    return(glob.oc.getgob(gobid));
	}

	public Coord2d getc() {
	    Gob gob;
	    try {
		if((gob = getgob()) != null) {
		    this.oa = gob.a;
		    return(new Coord2d(gob.getc()));
		}
	    } catch(Loading e) {}
	    this.oa = Double.NaN;
	    return(c);
	}

	void setc(Coord2d c) {
	    if((this.c != null) && (c != null))
		ma = this.c.angle(c);
	    this.c = c;
	}

	public double geta() {
	    return(Double.isNaN(oa) ? ma : oa);
	}
    }

    public enum TargetMark {
	SKULL("customclient/partyicons/targetskull", 1),
	CROSS("customclient/partyicons/targetcross", 2),
	MOON("customclient/partyicons/targetmoon", 3),
	TRIANGLE("customclient/partyicons/targettriangle", 4),
	DIAMOND("customclient/partyicons/targetdiamond", 5),
	STAR("customclient/partyicons/targetstar", 6),
	SQUARE("customclient/partyicons/targetsquare", 7),
	CIRCLE("customclient/partyicons/targetcircle", 8);

	public final String resPath;
	public final int order;

	TargetMark(String resPath, int order) {
	    this.resPath = resPath;
	    this.order = order;
	}

	public static TargetMark getByOrder(int order) {
	    for(TargetMark marker : values()) {
		if(marker.order == order)
		    return marker;
	    }
	    return null;
	}
    }

    private static final Pattern targetMarkerPattern = Pattern.compile("^(\\d)(?:,(\\d:-?\\d+))+$");
    public final Map<TargetMark, Long> targetMarkers = new EnumMap<>(TargetMark.class);
    private TargetMark nextMark = TargetMark.SKULL;

    {
	for(TargetMark targetMark : TargetMark.values())
	    targetMarkers.put(targetMark, -1L);
    }

    public String markNext(Gob gob) {
	synchronized(targetMarkers) {
	    clearTargetMarkerOverlays();
	    for(Map.Entry<TargetMark, Long> entry : targetMarkers.entrySet()) {
		if(entry.getValue().equals(gob.id))
		    entry.setValue(-1L);
	    }
	    targetMarkers.put(nextMark, gob.id);
	    return encodeMessage();
	}
    }

    public static boolean isTargetMarkerMessage(String message) {
	return (message != null) && targetMarkerPattern.matcher(message).matches();
    }

    public void handleMarkerMessage(String message) {
	if(!isTargetMarkerMessage(message))
	    return;
	synchronized(targetMarkers) {
	    clearTargetMarkerOverlays();
	    String[] marks = message.split(",");
	    int nextOrder = Integer.parseInt(marks[0]) + 1;
	    if(nextOrder > TargetMark.values().length)
		nextOrder = 1;
	    nextMark = TargetMark.getByOrder(nextOrder);
	    for(TargetMark targetMark : TargetMark.values())
		targetMarkers.put(targetMark, -1L);
	    for(int i = 1; i < marks.length; i++) {
		String[] markerData = marks[i].split(":");
		TargetMark mark = TargetMark.getByOrder(Integer.parseInt(markerData[0]));
		if(mark != null)
		    targetMarkers.put(mark, Long.valueOf(markerData[1]));
	    }
	    applyTargetMarkerOverlays();
	}
    }

    public void applyTargetMarker(Gob gob) {
	synchronized(targetMarkers) {
	    for(Map.Entry<TargetMark, Long> entry : targetMarkers.entrySet()) {
		if(entry.getValue().equals(gob.id)) {
		    gob.addTargetMarker(entry.getKey());
		    return;
		}
	    }
	    gob.removeTargetMarker();
	}
    }

    private void clearTargetMarkerOverlays() {
	for(Long id : targetMarkers.values()) {
	    Gob gob = glob.oc.getgob(id);
	    if(gob != null)
		gob.removeTargetMarker();
	}
    }

    private void applyTargetMarkerOverlays() {
	for(Map.Entry<TargetMark, Long> entry : targetMarkers.entrySet()) {
	    Gob gob = glob.oc.getgob(entry.getValue());
	    if(gob != null)
		gob.addTargetMarker(entry.getKey());
	}
    }

    private String encodeMessage() {
	StringBuilder message = new StringBuilder();
	message.append(nextMark.order);
	for(TargetMark mark : TargetMark.values())
	    message.append(',').append(mark.order).append(':').append(targetMarkers.get(mark));
	return message.toString();
    }
}
