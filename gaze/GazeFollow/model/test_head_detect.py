# Extract the head using OpenCV and Haars Cascades.
import cv2
upperbody_cascade = cv2.CascadeClassifier('/media/josephz/Data/cascades/haarcascade_mcs_upperbody.xml')
img = cv2.imread('test.jpg')
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

faces = upperbody_cascade.detectMultiScale(gray, scaleFactor=1.03,
        minNeighbors=10)
for (x,y,w,h) in faces:
    cv2.rectangle(img,(x,y),(x+w,y+h),(255,0,0),2)
    roi_gray = gray[y:y+h, x:x+w]
    roi_color = img[y:y+h, x:x+w]
cv2.imshow('img',img)
cv2.waitKey(0)
cv2.destroyAllWindows()

