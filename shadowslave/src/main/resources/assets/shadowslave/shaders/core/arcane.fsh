#version 150
in vec4 vertexColor;
in vec2 magicUV;
in float viewDistance;
uniform vec4 ColorModulator;
uniform float MagicTime;
uniform float MagicViewRange;
out vec4 fragColor;

// Analytic masks: no screen sampling, texture atlas, additional framebuffer or external shader pack.
float edge(float d, float thickness){
    float aa = max(fwidth(d), 0.008);
    return 1.0 - smoothstep(thickness-aa, thickness+aa, abs(d));
}
void main(){
    float material = floor((magicUV.x + 0.001) * 0.5);
    vec2 uv = vec2(magicUV.x-material*2.0, magicUV.y);
    vec2 p = uv-0.5;
    vec2 pixel = (floor(uv*16.0)+0.5)/16.0;
    float mask = 0.0;
    float core = 0.0;
    float shimmer = 0.92+0.08*sin(MagicTime*3.0+material*1.71);
    if(material<0.5){ // narrow ribbon seam and restrained halo
        mask = 1.0-smoothstep(0.20, 0.50, abs(p.y));
        core = edge(p.y, 0.085);
    }else if(material<1.5){ // pixel diamond mote
        float diamond = abs(pixel.x-0.5)+abs(pixel.y-0.5);
        mask = 1.0-smoothstep(0.24, 0.50, diamond);
        core = 1.0-smoothstep(0.08, 0.23, diamond);
    }else if(material<2.5){ // stepped tapered flame tongue
        float y = pixel.y;
        float center = 0.5+sin(y*7.0+MagicTime*5.0)*0.065*y;
        float width = 0.32*(1.0-y)+0.025;
        mask = (1.0-smoothstep(width*0.62, width, abs(pixel.x-center)))*smoothstep(0.0, 0.13, y);
        core = mask*(1.0-y)*(1.0-smoothstep(0.025, width*0.7, abs(pixel.x-center)));
    }else if(material<3.5){ // angular crystalline rhombus
        float diamond = abs(p.x)*1.45+abs(p.y);
        mask = 1.0-smoothstep(0.43, 0.49, diamond);
        core = edge(diamond-0.40, 0.032)+edge(p.x+p.y*0.28, 0.025)*0.45;
        mask *= p.x+p.y*0.28>0.0 ? 0.88 : 0.50;
    }else if(material<4.5){ // engraved glyph stroke
        mask = (1.0-smoothstep(0.30, 0.50, abs(p.y)))*(1.0-smoothstep(0.43, 0.50, abs(p.x)));
        core = edge(p.y, 0.15);
    }else if(material<5.5){ // shadow wisp curling into a fine tail
        float center = 0.5+sin(uv.y*5.0+MagicTime*1.4)*0.14;
        float width = 0.05+0.25*(1.0-uv.y);
        mask = (1.0-smoothstep(width*0.2, width, abs(uv.x-center)))*smoothstep(0.0, 0.10, uv.y)*(1.0-uv.y*0.7);
        core = mask*0.12;
    }else if(material<6.5){ // moving broken foam highlights
        mask = 1.0-smoothstep(0.19, 0.50, abs(p.y));
        float foam = sin(uv.x*18.0+MagicTime*4.0)*0.10;
        core = edge(p.y+foam, 0.07)*0.75;
    }else if(material<7.5){ // pointed leaf and root strip
        float leaf = abs(pixel.x-0.5)/max(0.035, sin(pixel.y*3.141593)*0.34);
        mask = (1.0-smoothstep(0.70, 1.0, leaf))*smoothstep(0.0, 0.08, uv.y);
        core = edge(p.x, 0.025)*mask*0.65;
    }else{ // engraved rings, rune ticks and sparse center star
        float radius = length(p)*2.0;
        float angle = atan(p.y, p.x);
        float inner = edge(radius-0.56, 0.012);
        float outer = edge(radius-0.91, 0.015);
        float ticks = pow(max(0.0, cos(angle*16.0)), 12.0)*edge(radius-0.77, 0.055);
        float star = edge(sin(angle*4.0)*radius, 0.025)*smoothstep(0.06, 0.16, radius)*(1.0-smoothstep(0.38, 0.48, radius));
        core = max(max(inner, outer), max(ticks*0.75, star*0.45));
        mask = core+edge(radius-0.91, 0.040)*0.18;
    }
    mask = clamp(mask, 0.0, 1.0);
    float fade = 1.0-smoothstep(MagicViewRange*0.75, MagicViewRange, viewDistance);
    float alpha = vertexColor.a*ColorModulator.a*mask*fade*shimmer;
    if(alpha<0.002) discard;
    vec3 emission = mix(vertexColor.rgb, vec3(1.0, 0.98, 0.91), clamp(core,0.0,1.0)*0.38);
    if(material>4.5 && material<5.5){
        // Standard alpha blending for this material creates actual matte shadow, bounded by a colored rim.
        float rim = smoothstep(0.18, 0.40, abs(uv.x-0.5));
        emission = mix(vec3(0.022,0.025,0.040), vertexColor.rgb*0.56, rim);
    }
    fragColor = vec4(emission*ColorModulator.rgb, alpha);
}
