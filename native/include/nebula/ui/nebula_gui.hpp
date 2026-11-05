#pragma once

struct ImFont;

namespace nebula {
namespace gui {

void initTheme();
void setFonts(ImFont* body, ImFont* display);
void renderGui();
void renderHud();
void renderNotifications();
void renderDamageIndicator();

} 
} 

