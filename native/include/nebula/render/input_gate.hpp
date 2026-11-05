#pragma once
#include <windows.h>
namespace nebula::render {
inline bool releaseMessage(UINT message) {
    return message == WM_KEYUP || message == WM_SYSKEYUP || message == WM_LBUTTONUP ||
           message == WM_RBUTTONUP || message == WM_MBUTTONUP || message == WM_XBUTTONUP;
}
inline bool blockedGameInput(UINT message) {
    return message == WM_INPUT || (!releaseMessage(message) &&
                                   ((message >= WM_MOUSEFIRST && message <= WM_MOUSELAST) ||
                                    (message >= WM_KEYFIRST && message <= WM_KEYLAST) || message == WM_CHAR));
}
} 
