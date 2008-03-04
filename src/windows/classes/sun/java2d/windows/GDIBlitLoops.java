/*
 * Copyright 2002-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.java2d.windows;

import java.awt.Composite;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.loops.GraphicsPrimitiveMgr;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.Blit;
import sun.java2d.pipe.Region;
import sun.java2d.SurfaceData;

/**
 * GDIBlitLoops
 *
 * This class accelerates Blits between certain surfaces and the
 * screen, using GDI.  The reason for these loops is to find
 * a way of copying to the screen without using DDraw locking
 * that is faster than our current fallback (which creates
 * a temporary GDI DIB)
 */
public class GDIBlitLoops extends Blit {

    // Store these values to be passed to native code
    int rmask, gmask, bmask;

    // Needs lookup table (for indexed color image copies)
    boolean indexed = false;

    /**
     * Note that we do not register loops to 8-byte destinations.  This
     * is due to faster processing of dithering through our software
     * loops than through GDI StretchBlt processing.
     */
    public static void register()
    {
        GraphicsPrimitive[] primitives = {
            new GDIBlitLoops(SurfaceType.IntRgb,
                             Win32SurfaceData.AnyGdi),
            new GDIBlitLoops(SurfaceType.Ushort555Rgb,
                             Win32SurfaceData.AnyGdi,
                             0x7C00, 0x03E0, 0x001F),
            new GDIBlitLoops(SurfaceType.Ushort565Rgb,
                             Win32SurfaceData.AnyGdi,
                             0xF800, 0x07E0, 0x001F),
            new GDIBlitLoops(SurfaceType.ThreeByteBgr,
                             Win32SurfaceData.AnyGdi),
            new GDIBlitLoops(SurfaceType.ByteIndexedOpaque,
                             Win32SurfaceData.AnyGdi,
                             true),
            new GDIBlitLoops(SurfaceType.Index8Gray,
                             Win32SurfaceData.AnyGdi,
                             true),
            new GDIBlitLoops(SurfaceType.ByteGray,
                             Win32SurfaceData.AnyGdi),
        };
        GraphicsPrimitiveMgr.register(primitives);
    }

    /**
     * This constructor exists for srcTypes that have no need of
     * component masks. GDI only expects masks for 2- and 4-byte
     * DIBs, so all 1- and 3-byte srcTypes can skip the mask setting.
     */
    public GDIBlitLoops(SurfaceType srcType, SurfaceType dstType) {
        this(srcType, dstType, 0, 0, 0);
    }

    /**
     * This constructor exists for srcTypes that need lookup tables
     * during image copying.
     */
    public GDIBlitLoops(SurfaceType srcType, SurfaceType dstType,
                        boolean indexed)
    {
        this(srcType, dstType, 0, 0, 0);
        this.indexed = indexed;
    }

    /**
     * This constructor sets mask for this primitive which can be
     * retrieved in native code to set the appropriate values for GDI.
     */
    public GDIBlitLoops(SurfaceType srcType, SurfaceType dstType,
                        int rmask, int gmask, int bmask)
    {
        super(srcType, CompositeType.SrcNoEa, dstType);
        this.rmask = rmask;
        this.gmask = gmask;
        this.bmask = bmask;
    }

    /**
     * nativeBlit
     * This native method is where all of the work happens in the
     * accelerated Blit.
     */
    public native void nativeBlit(SurfaceData src, SurfaceData dst,
                                  Region clip,
                                  int sx, int sy, int dx, int dy,
                                  int w, int h,
                                  int rmask, int gmask, int bmask,
                                  boolean needLut);

    /**
     * Blit
     * This method wraps the nativeBlit call, sending in additional
     * info on whether the native method needs to get LUT info
     * from the source image.  Note that we do not pass in the
     * Composite data because we only register these loops for
     * SrcNoEa composite operations.
     */
    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        nativeBlit(src, dst, clip, sx, sy, dx, dy, w, h,
                   rmask, gmask, bmask, indexed);
    }


}
