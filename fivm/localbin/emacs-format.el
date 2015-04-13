;;; emacs batch formatting

(defun emacs-format-function ()
   "Format the whole buffer."
   (c-set-style "stroustrup")
   (indent-region (point-min) (point-max) nil)
   (untabify (point-min) (point-max))
   (save-buffer)
)
